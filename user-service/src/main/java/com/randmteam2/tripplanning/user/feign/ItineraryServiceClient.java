package com.randmteam2.tripplanning.user.feign;

import com.randmteam2.tripplanning.user.dto.UserTripSummaryAggregateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "itinerary-service", url = "${feign.itinerary-service.url}")
public interface ItineraryServiceClient {

    @GetMapping("/api/itineraries/user/{userId}/summary")
    UserTripSummaryAggregateDTO getUserItinerarySummary(@PathVariable Long userId);

    @GetMapping("/api/itineraries/user/{userId}/active-count")
    int getActiveItineraryCount(@PathVariable Long userId);

    @GetMapping("/api/itineraries/user/{userId}/completed-count")
    long getCompletedItineraryCount(@PathVariable Long userId);
}
