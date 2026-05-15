package com.randmteam2.tripplanning.user.feign;

import com.randmteam2.tripplanning.user.dto.UserBookingTotalDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "booking-service", url = "${feign.booking-service.url}")
public interface BookingServiceClient {

    @GetMapping("/api/bookings/user/{userId}/total")
    UserBookingTotalDTO getUserBookingTotal(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate
    );
}
