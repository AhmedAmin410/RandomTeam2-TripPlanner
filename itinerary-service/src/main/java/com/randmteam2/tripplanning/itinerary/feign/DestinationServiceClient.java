package com.randmteam2.tripplanning.itinerary.feign;

import com.randmteam2.tripplanning.itinerary.dto.DestinationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "destination-service", url = "${feign.destination-service.url}")
public interface DestinationServiceClient {
    @GetMapping("/api/destinations/{id}")
    DestinationDTO getDestination(@PathVariable Long id);
}