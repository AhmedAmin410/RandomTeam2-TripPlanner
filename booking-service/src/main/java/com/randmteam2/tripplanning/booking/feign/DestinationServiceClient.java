package com.randmteam2.tripplanning.booking.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "destination-service", contextId = "bookingDestinationServiceClient",
        url = "${feign.destination-service.url}")
public interface DestinationServiceClient {

    @PostMapping("/api/destinations/batch")
    List<Map<String, Object>> batchGetDestinations(@RequestBody Map<String, Object> request);
}