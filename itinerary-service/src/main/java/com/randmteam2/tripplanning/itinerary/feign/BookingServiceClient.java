package com.randmteam2.tripplanning.itinerary.feign;

import com.randmteam2.tripplanning.itinerary.dto.BookingAggregateDTO;
import com.randmteam2.tripplanning.itinerary.dto.BookingConfirmedSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "booking-service", url = "${feign.booking-service.url}")
public interface BookingServiceClient {
    @GetMapping("/api/bookings/itinerary/{id}/confirmed-count")
    BookingConfirmedSummaryDTO getConfirmedSummary(@PathVariable Long id);

    @PostMapping("/api/bookings/aggregate-by-itineraries")
    List<BookingAggregateDTO> aggregateByItineraries(@RequestBody List<Long> itineraryIds);
}