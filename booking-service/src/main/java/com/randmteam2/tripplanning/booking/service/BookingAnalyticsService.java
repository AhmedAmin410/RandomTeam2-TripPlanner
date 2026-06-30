package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.DestinationSeasonRevenueDTO;
import com.randmteam2.tripplanning.booking.feign.BookingFeignClients;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.mongo.EventFactory;
import com.randmteam2.tripplanning.booking.mongo.EventType;
import com.randmteam2.tripplanning.booking.mongo.MongoEvent;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEventRepository;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import com.randmteam2.tripplanning.contracts.dto.DestinationSummaryDTO;
import com.randmteam2.tripplanning.contracts.dto.ItinerarySummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BookingAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(BookingAnalyticsService.class);

    private final BookingRepository bookingRepository;
    private final PaymentAuditEventRepository auditRepository;
    private final BookingFeignClients.ItineraryServiceSafeClient itineraryServiceSafeClient;
    private final BookingFeignClients.DestinationServiceSafeClient destinationServiceSafeClient;

    public BookingAnalyticsService(
            BookingRepository bookingRepository,
            PaymentAuditEventRepository auditRepository,
            BookingFeignClients.ItineraryServiceSafeClient itineraryServiceSafeClient,
            BookingFeignClients.DestinationServiceSafeClient destinationServiceSafeClient) {
        this.bookingRepository = bookingRepository;
        this.auditRepository = auditRepository;
        this.itineraryServiceSafeClient = itineraryServiceSafeClient;
        this.destinationServiceSafeClient = destinationServiceSafeClient;
    }

    /**
     * S5-F10 — Revenue by Destination and Season.
     * M3: two Feign batch calls replace the old 3-table SQL JOIN.
     * Cached 10 minutes.
     */
    @Cacheable(value = "booking-service", key = "'S5-F10::' + #startDate + '::' + #endDate")
    public List<DestinationSeasonRevenueDTO> getRevenueByDestinationAndSeason(
            LocalDate startDate, LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must be before or equal to endDate");
        }

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to   = endDate.atTime(23, 59, 59, 999_000_000);

        // 1. Fetch confirmed bookings locally — no cross-service SQL
        List<Booking> bookings = bookingRepository.findConfirmedBookingsInRange(from, to);
        if (bookings.isEmpty()) {
            return List.of();
        }

        // 2. Collect distinct itineraryIds
        List<Long> itineraryIds = bookings.stream()
                .map(Booking::getItineraryId)
                .distinct()
                .toList();

        // 3. Feign batch → itinerary-service: build itineraryId → destinationId map
        Map<Long, Long> itineraryToDestination = new HashMap<>();
        try {
            List<ItinerarySummaryDTO> itineraries =
                    itineraryServiceSafeClient.batchGetItineraries(itineraryIds);
            for (ItinerarySummaryDTO it : itineraries) {
                if (it.destinationId() != null) {
                    itineraryToDestination.put(it.itineraryId(), it.destinationId());
                }
            }
        } catch (Exception e) {
            log.warn("Feign call to itinerary-service failed for S5-F10: {}", e.getMessage());
            return List.of();
        }

        if (itineraryToDestination.isEmpty()) {
            return List.of();
        }

        // 4. Collect distinct destinationIds
        List<Long> destinationIds = itineraryToDestination.values().stream()
                .distinct()
                .toList();

        // 5. Feign batch → destination-service: build destinationId → name map
        Map<Long, String> destinationNames = new HashMap<>();
        try {
            List<DestinationSummaryDTO> destinations =
                    destinationServiceSafeClient.batchGetDestinations(destinationIds);
            for (DestinationSummaryDTO d : destinations) {
                destinationNames.put(d.destinationId(), d.name());
            }
        } catch (Exception e) {
            log.warn("Feign call to destination-service failed for S5-F10: {}", e.getMessage());
            return List.of();
        }

        // 6. Group bookings by destinationId and aggregate revenue stats
        Map<Long, List<Booking>> byDestination = new HashMap<>();
        for (Booking b : bookings) {
            Long destId = itineraryToDestination.get(b.getItineraryId());
            if (destId == null) continue;
            byDestination.computeIfAbsent(destId, k -> new ArrayList<>()).add(b);
        }

        return byDestination.entrySet().stream().map(entry -> {
                    Long destId = entry.getKey();
                    List<Booking> group = entry.getValue();

                    double totalRevenue    = 0;
                    double surchargeRevenue = 0;
                    long   peakCount       = 0;
                    long   offPeakCount    = 0;

                    for (Booking b : group) {
                        totalRevenue += b.getAmount();
                        double surcharge = 0;
                        if (b.getBookingDetails() != null) {
                            Object s = b.getBookingDetails().get("seasonalSurcharge");
                            if (s instanceof Number n) surcharge = n.doubleValue();
                        }
                        surchargeRevenue += surcharge;
                        if (surcharge > 0) peakCount++; else offPeakCount++;
                    }

                    return DestinationSeasonRevenueDTO.builder()
                            .destinationId(destId)
                            .destinationName(destinationNames.getOrDefault(destId, "Unknown"))
                            .totalRevenue(totalRevenue)
                            .surchargeRevenue(surchargeRevenue)
                            .baseRevenue(totalRevenue - surchargeRevenue)
                            .peakBookingCount(peakCount)
                            .offPeakBookingCount(offPeakCount)
                            .build();
                })
                .sorted(Comparator.comparingDouble(DestinationSeasonRevenueDTO::getTotalRevenue).reversed())
                .toList();
    }

    /**
     * Logs ANALYTICS_VIEWED to MongoDB — called outside the cached method
     * so it fires on every invocation including cache hits (M2 §10.5.1).
     * ANALYTICS_VIEWED must NOT trigger cache invalidation (M2 §4.4.4).
     */
    public void logAnalyticsViewed() {
        try {
            MongoEvent ev = EventFactory.createEvent(EventType.PAYMENT_AUDIT, Map.of(
                    "action", "ANALYTICS_VIEWED",
                    "timestamp", LocalDateTime.now(),
                    "details", Map.of("endpoint", "S5-F10")
            ));
            auditRepository.save((com.randmteam2.tripplanning.booking.mongo.PaymentAuditEvent) ev);
        } catch (Exception e) {
            log.warn("MongoDB analytics log failed: {}", e.getMessage());
        }
    }
}
