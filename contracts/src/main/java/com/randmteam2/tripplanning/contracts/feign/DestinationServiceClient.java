package com.randmteam2.tripplanning.contracts.feign;

import com.randmteam2.tripplanning.contracts.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "destination-service", url = "${feign.destination-service.url}")
public interface DestinationServiceClient {

    @GetMapping("/api/destinations/{id}")
    Object getDestination(@PathVariable("id") Long id);

    @PostMapping("/api/destinations/batch")
    List<DestinationSummaryDTO> batchGetDestinations(@RequestBody BatchDestinationRequest request);
}
