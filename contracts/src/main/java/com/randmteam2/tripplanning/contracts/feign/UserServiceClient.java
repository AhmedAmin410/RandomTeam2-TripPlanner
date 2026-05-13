package com.randmteam2.tripplanning.contracts.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "${feign.user-service.url}")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    Object getUser(@PathVariable Long userId);
}
