package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.DestinationSeasonRevenueDTO;
import com.randmteam2.tripplanning.booking.feign.DestinationServiceClient;
import com.randmteam2.tripplanning.booking.feign.ItineraryServiceClient;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.mongo.EventFactory;
import com.randmteam2.tripplanning.booking.mongo.EventType;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEvent;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEventRepository;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
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
    private final ItineraryServiceClient itineraryServiceClient;
    private final DestinationServiceClient destinationServiceClient;

    public BookingAnalyticsService(BookingRepository bookingRepository,
                                   PaymentAuditEventRepository auditRepository,
                                   ItineraryServiceClient itineraryServiceClient,
                                   DestinationServiceClient destinationServiceClient) {
        this.bookingRepository = bookingRepository;
        this.auditRepository = auditRepository;
        this.itineraryServiceClient = itineraryServiceClient;
        this.destinationServiceClient = destinationServiceClient;
    }

    @Cacheable(value = "booking-service", key = "'S5-F10::' + #startDate + '::' + #endDate")
    public List<DestinationSeasonRevenueDTO> getRevenueByDestinationAndSeason(
            LocalDate startDate, LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must be before or equal to endDate");
        }

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to   = endDate.atTime(23, 59, 59, 999_000_000);

        // 1. Query local bookings (no cross-service join)
        List<Booking> bookings = bookingRepository.findConfirmedBookingsInRange(from, to);
        if (bookings.isEmpty()) {
            return List.of();
        }

        // 2. Collect distinct itineraryIds
        List<Long> itineraryIds = bookings.stream()
                .map(Booking::getItineraryId)
                .distinct()
                .toList();

        // 3. Batch fetch itineraries → Map<itineraryId, destinationId>
        Map<Long, Long> itineraryToDestination = new HashMap<>();
        try {
            List<Map<String, Object>> itineraries = itineraryServiceClient.batchGetItineraries(
                    Map.of("itineraryIds", itineraryIds));
            for (Map<String, Object> it : itineraries) {
                Long itId   = ((Number) it.get("itineraryId")).longValue();
                Long destId = ((Number) it.get("destinationId")).longValue();
                itineraryToDestination.put(itId, destId);
            }
        } catch (Exception e) {
            log.warn("Failed to batch-fetch itineraries for S5-F10: {}", e.getMessage());
            return List.of();
        }

        // 4. Collect distinct destinationIds
        List<Long> destinationIds = itineraryToDestination.values().stream().distinct().toList();

        // 5. Batch fetch destinations → Map<destinationId, name>
        Map<Long, String> destinationNames = new HashMap<>();
        try {
            List<Map<String, Object>> destinations = destinationServiceClient.batchGetDestinations(
                    Map.of("destinationIds", destinationIds));
            for (Map<String, Object> d : destinations) {
                Long destId = ((Number) d.get("destinationId")).longValue();
                String name = (String) d.get("name");
                destinationNames.put(destId, name);
            }
        } catch (Exception e) {
            log.warn("Failed to batch-fetch destinations for S5-F10: {}", e.getMessage());
            return List.of();
        }

        // 6. Group bookings by destinationId and compute revenue stats
        Map<Long, List<Booking>> byDestination = new HashMap<>();
        for (Booking b : bookings) {
            Long destId = itineraryToDestination.get(b.getItineraryId());
            if (destId == null) continue;
            byDestination.computeIfAbsent(destId, k -> new ArrayList<>()).add(b);
        }

        return byDestination.entrySet().stream().map(entry -> {
            Long destId = entry.getKey();
            List<Booking> group = entry.getValue();

            double totalRevenue = 0;
            double surchargeRevenue = 0;
            long peakCount = 0;
            long offPeakCount = 0;

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

    public void logAnalyticsViewed() {
        try {
            PaymentAuditEvent ev = (PaymentAuditEvent) EventFactory.createEvent(
                    EventType.PAYMENT_AUDIT,
                    Map.of("action", "ANALYTICS_VIEWED",
                           "timestamp", LocalDateTime.now(),
                           "details", Map.of("endpoint", "S5-F10")));
            auditRepository.save(ev);
        } catch (Exception e) {
            System.err.println("[WARN] MongoDB analytics log failed: " + e.getMessage());
        }
    }
}