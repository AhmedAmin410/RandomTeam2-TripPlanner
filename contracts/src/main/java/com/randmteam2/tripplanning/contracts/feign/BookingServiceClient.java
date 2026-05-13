package com.randmteam2.tripplanning.contracts.feign;

import com.randmteam2.tripplanning.contracts.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "booking-service", url = "${feign.booking-service.url}")
public interface BookingServiceClient {

    @GetMapping("/api/bookings/user/{userId}/total")
    UserBookingTotalDTO getUserBookingTotal(
        @PathVariable Long userId,
        @RequestParam String startDate,
        @RequestParam String endDate
    );

    @PostMapping("/api/bookings/aggregate-by-itineraries")
    ItineraryBookingAggregateDTO aggregateByItineraries(@RequestBody Object request);

    @GetMapping("/api/bookings/itinerary/{itineraryId}/confirmed-summary")
    ConfirmedSummaryDTO getConfirmedSummary(@PathVariable Long itineraryId);
}
