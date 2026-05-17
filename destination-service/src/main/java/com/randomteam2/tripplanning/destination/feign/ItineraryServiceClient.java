package com.randomteam2.tripplanning.destination.feign;

import com.randomteam2.tripplanning.destination.dto.DestinationBookingRevenueAggregateDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationDashboardAggregateDTO;
import com.randomteam2.tripplanning.destination.dto.ItineraryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "itinerary-service", url = "${feign.itinerary-service.url}")
public interface ItineraryServiceClient {

    @GetMapping("/api/itineraries/destination/{destinationId}/booking-revenue")
    DestinationBookingRevenueAggregateDTO getDestinationBookingRevenue(
            @PathVariable Long destinationId,
            @RequestParam String startDate,
            @RequestParam String endDate
    );

    @GetMapping("/api/itineraries/destination/{destinationId}/active-count")
    Integer getDestinationActiveItineraryCount(@PathVariable Long destinationId);

    @GetMapping("/api/itineraries/{itineraryId}")
    ItineraryDTO getItinerary(@PathVariable Long itineraryId);

    /**
     * S2-F12 (M3): Fetches aggregated itinerary stats for a destination.
     * Endpoint exposed by itinerary-service:
     *   GET /api/itineraries/destination/{destinationId}/dashboard-aggregate
     *
     * Returns totalItineraries, completedItineraries (STATUS_COMPLETED_FAMILY),
     * and totalVisitors (distinct userId).
     */
    @GetMapping("/api/itineraries/destination/{destinationId}/dashboard-aggregate")
    DestinationDashboardAggregateDTO getDestinationDashboardAggregate(@PathVariable Long destinationId);
}