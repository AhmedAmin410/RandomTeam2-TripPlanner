package com.randmteam2.tripplanning.booking.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "itinerary-service", contextId = "bookingItineraryServiceClient",
        url = "${feign.itinerary-service.url}")
public interface ItineraryServiceClient {

    @GetMapping("/api/itineraries/{itineraryId}")
    Map<String, Object> getItinerary(@PathVariable Long itineraryId);

    @GetMapping("/api/itineraries/destination/{destinationId}/active-count")
    int getDestinationActiveCount(@PathVariable Long destinationId);

    @PostMapping("/api/itineraries/batch")
    List<Map<String, Object>> batchGetItineraries(@RequestBody Map<String, Object> request);
}