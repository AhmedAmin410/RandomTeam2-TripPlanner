package com.randmteam2.tripplanning.contracts.feign;

import com.randmteam2.tripplanning.contracts.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "itinerary-service", url = "${feign.itinerary-service.url}")
public interface ItineraryServiceClient {

    @GetMapping("/api/itineraries/user/{userId}/summary")
    UserTripSummaryAggregateDTO getUserItinerarySummary(@PathVariable("userId") Long userId);

    @GetMapping("/api/itineraries/user/{userId}/active-count")
    int getActiveItineraryCount(@PathVariable("userId") Long userId);

    @GetMapping("/api/itineraries/user/{userId}/completed-count")
    long getCompletedItineraryCount(@PathVariable("userId") Long userId);

    @GetMapping("/api/itineraries/destination/{destinationId}/booking-revenue")
    DestinationBookingRevenueAggregateDTO getDestinationBookingRevenue(
        @PathVariable("destinationId") Long destinationId,
        @RequestParam("startDate") String startDate,
        @RequestParam("endDate") String endDate
    );

    @GetMapping("/api/itineraries/destination/{destinationId}/active-count")
    int getDestinationActiveItineraryCount(@PathVariable("destinationId") Long destinationId);

    @GetMapping("/api/itineraries/destination/{destinationId}/dashboard-aggregate")
    DestinationDashboardAggregateDTO getDestinationDashboardAggregate(@PathVariable("destinationId") Long destinationId);

    @GetMapping("/api/itineraries/{itineraryId}")
    Object getItinerary(@PathVariable("itineraryId") Long itineraryId);

    @PostMapping("/api/itineraries/batch")
    List<ItinerarySummaryDTO> batchGetItineraries(@RequestBody BatchItineraryRequest request);
}
