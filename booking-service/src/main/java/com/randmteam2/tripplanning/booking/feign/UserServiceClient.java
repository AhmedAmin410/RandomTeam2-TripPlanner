package com.randmteam2.tripplanning.booking.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "user-service", contextId = "bookingUserServiceClient",
        url = "${feign.user-service.url}")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    Map<String, Object> getUser(@PathVariable Long userId);
}