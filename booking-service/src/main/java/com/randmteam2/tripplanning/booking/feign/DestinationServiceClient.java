package com.randmteam2.tripplanning.booking.feign;

import com.randmteam2.tripplanning.contracts.dto.BatchDestinationRequest;
import com.randmteam2.tripplanning.contracts.dto.DestinationSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "destination-service", contextId = "bookingDestinationServiceClient",
        url = "${feign.destination-service.url}")
public interface DestinationServiceClient {

    /**
     * Used by S5-F10 batch: maps destinationId → name/country/category.
     */
    @PostMapping("/api/destinations/batch")
    List<DestinationSummaryDTO> batchGetDestinations(@RequestBody BatchDestinationRequest request);
}