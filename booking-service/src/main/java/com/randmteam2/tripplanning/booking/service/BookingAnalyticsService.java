package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.DestinationSeasonRevenueDTO;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.mongo.EventFactory;
import com.randmteam2.tripplanning.booking.mongo.EventType;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEvent;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEventRepository;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import com.randmteam2.tripplanning.contracts.dto.BatchDestinationRequest;
import com.randmteam2.tripplanning.contracts.dto.BatchItineraryRequest;
import com.randmteam2.tripplanning.contracts.dto.DestinationSummaryDTO;
import com.randmteam2.tripplanning.contracts.dto.ItinerarySummaryDTO;
import com.randmteam2.tripplanning.contracts.feign.DestinationServiceClient;
import com.randmteam2.tripplanning.contracts.feign.ItineraryServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BookingAnalyticsService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentAuditEventRepository auditRepository;
    @Autowired private ItineraryServiceClient itineraryServiceClient;
    @Autowired private DestinationServiceClient destinationServiceClient;

    @Cacheable(value = "booking-service", key = "'S5-F10::' + #startDate + '::' + #endDate")
    public List<DestinationSeasonRevenueDTO> getRevenueByDestinationAndSeason(
            LocalDate startDate, LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must be before or equal to endDate");
        }

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to   = endDate.atTime(23, 59, 59, 999_000_000);

        List<Booking> bookings = bookingRepository.findConfirmedBookingsInDateRange(from, to);
        if (bookings.isEmpty()) {
            return List.of();
        }

        Map<Long, ItinerarySummaryDTO> itinerariesById = loadItineraries(bookings);
        Map<Long, DestinationSummaryDTO> destinationsById = loadDestinations(itinerariesById.values());
        Map<Long, RevenueAccumulator> totalsByDestination = new LinkedHashMap<>();

        for (Booking booking : bookings) {
            ItinerarySummaryDTO itinerary = itinerariesById.get(booking.getItineraryId());
            if (itinerary == null || itinerary.destinationId() == null) {
                continue;
            }

            RevenueAccumulator total = totalsByDestination.computeIfAbsent(
                    itinerary.destinationId(), RevenueAccumulator::new);
            total.add(booking.getAmount(), seasonalSurcharge(booking));
        }

        return totalsByDestination.values().stream()
                .sorted((left, right) -> Double.compare(right.totalRevenue, left.totalRevenue))
                .map(total -> {
                    DestinationSummaryDTO destination = destinationsById.get(total.destinationId);
                    String name = destination != null ? destination.name() : "Destination " + total.destinationId;
                    return DestinationSeasonRevenueDTO.builder()
                            .destinationId(total.destinationId)
                            .destinationName(name)
                            .totalRevenue(total.totalRevenue)
                            .surchargeRevenue(total.surchargeRevenue)
                            .baseRevenue(total.totalRevenue - total.surchargeRevenue)
                            .peakBookingCount(total.peakBookingCount)
                            .offPeakBookingCount(total.offPeakBookingCount)
                            .build();
                })
                .toList();
    }

    // called OUTSIDE @Cacheable so it fires on every request including cache hits
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

    private Map<Long, ItinerarySummaryDTO> loadItineraries(List<Booking> bookings) {
        List<Long> itineraryIds = bookings.stream()
                .map(Booking::getItineraryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (itineraryIds.isEmpty()) {
            return Map.of();
        }

        List<ItinerarySummaryDTO> itineraries =
                itineraryServiceClient.batchGetItineraries(new BatchItineraryRequest(itineraryIds));
        if (itineraries == null) {
            return Map.of();
        }
        return itineraries.stream()
                .filter(itinerary -> itinerary.itineraryId() != null)
                .collect(Collectors.toMap(
                        ItinerarySummaryDTO::itineraryId,
                        Function.identity(),
                        (first, ignored) -> first,
                        HashMap::new));
    }

    private Map<Long, DestinationSummaryDTO> loadDestinations(Collection<ItinerarySummaryDTO> itineraries) {
        List<Long> destinationIds = itineraries.stream()
                .map(ItinerarySummaryDTO::destinationId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (destinationIds.isEmpty()) {
            return Map.of();
        }

        List<DestinationSummaryDTO> destinations =
                destinationServiceClient.batchGetDestinations(new BatchDestinationRequest(destinationIds));
        if (destinations == null) {
            return Map.of();
        }
        return destinations.stream()
                .filter(destination -> destination.destinationId() != null)
                .collect(Collectors.toMap(
                        DestinationSummaryDTO::destinationId,
                        Function.identity(),
                        (first, ignored) -> first,
                        HashMap::new));
    }

    private double seasonalSurcharge(Booking booking) {
        Map<String, Object> details = booking.getBookingDetails();
        if (details == null) {
            return 0.0;
        }

        Object value = details.get("seasonalSurcharge");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private static class RevenueAccumulator {
        private final Long destinationId;
        private double totalRevenue;
        private double surchargeRevenue;
        private long peakBookingCount;
        private long offPeakBookingCount;

        private RevenueAccumulator(Long destinationId) {
            this.destinationId = destinationId;
        }

        private void add(Double amount, double surcharge) {
            totalRevenue += amount != null ? amount : 0.0;
            surchargeRevenue += surcharge;
            if (surcharge > 0) {
                peakBookingCount++;
            } else {
                offPeakBookingCount++;
            }
        }
    }
}
