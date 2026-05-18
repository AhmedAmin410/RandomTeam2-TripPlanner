package com.randmteam2.tripplanning.itinerary.feign;

import com.randmteam2.tripplanning.itinerary.dto.BookingAggregateDTO;
import com.randmteam2.tripplanning.itinerary.dto.BookingConfirmedSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "booking-service", url = "${feign.booking-service.url}")
public interface BookingServiceClient {

    @GetMapping("/api/bookings/itinerary/{id}/confirmed-summary")
    BookingConfirmedSummaryDTO getConfirmedSummary(@PathVariable Long id);

    /**
     * Body: { "itineraryIds": [...], "startDate": "...", "endDate": "...", "status": "CONFIRMED" }
     */
    @PostMapping("/api/bookings/aggregate-by-itineraries")
    BookingAggregateDTO aggregateByItineraries(@RequestBody Map<String, Object> request);
}