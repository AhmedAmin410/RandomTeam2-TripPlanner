package com.randmteam2.tripplanning.booking.feign;

import com.randmteam2.tripplanning.contracts.dto.BatchItineraryRequest;
import com.randmteam2.tripplanning.contracts.dto.ConfirmedSummaryDTO;
import com.randmteam2.tripplanning.contracts.dto.ItinerarySummaryDTO;
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

    /**
     * Used by S5-F4 (status validation) and S5-F12 (refund strategy input).
     * Returns Map so we can safely read status, startDate, destinationId.
     */
    @GetMapping("/api/itineraries/{itineraryId}")
    Map<String, Object> getItinerary(@PathVariable Long itineraryId);

    /**
     * Used by S5-F4 to compute seasonalSurcharge (M2 §4.6).
     */
    @GetMapping("/api/itineraries/destination/{destinationId}/active-count")
    int getDestinationActiveItineraryCount(@PathVariable Long destinationId);

    /**
     * Used by S5-F10 batch: maps itineraryId → destinationId.
     */
    @PostMapping("/api/itineraries/batch")
    List<ItinerarySummaryDTO> batchGetItineraries(@RequestBody BatchItineraryRequest request);
}