package com.randmteam2.tripplanning.contracts.feign;

import com.randmteam2.tripplanning.contracts.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "itinerary-service", url = "${feign.itinerary-service.url}")
public interface ItineraryServiceClient {

    @GetMapping("/api/itineraries/user/{userId}/summary")
    UserTripSummaryAggregateDTO getUserItinerarySummary(@PathVariable Long userId);

    @GetMapping("/api/itineraries/user/{userId}/active-count")
    int getActiveItineraryCount(@PathVariable Long userId);

    @GetMapping("/api/itineraries/user/{userId}/completed-count")
    long getCompletedItineraryCount(@PathVariable Long userId);

    @GetMapping("/api/itineraries/destination/{destinationId}/booking-revenue")
    DestinationBookingRevenueAggregateDTO getDestinationBookingRevenue(
        @PathVariable Long destinationId,
        @RequestParam String startDate,
        @RequestParam String endDate
    );

    @GetMapping("/api/itineraries/destination/{destinationId}/active-count")
    int getDestinationActiveItineraryCount(@PathVariable Long destinationId);

    @GetMapping("/api/itineraries/destination/{destinationId}/dashboard-aggregate")
    DestinationDashboardAggregateDTO getDestinationDashboardAggregate(@PathVariable Long destinationId);

    @GetMapping("/api/itineraries/{itineraryId}")
    Object getItinerary(@PathVariable Long itineraryId);

    @PostMapping("/api/itineraries/batch")
    List<ItinerarySummaryDTO> batchGetItineraries(@RequestBody BatchItineraryRequest request);
}
