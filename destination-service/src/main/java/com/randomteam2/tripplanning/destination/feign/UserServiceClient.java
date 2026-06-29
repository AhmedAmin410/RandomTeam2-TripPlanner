package com.randomteam2.tripplanning.destination.feign;

import com.randomteam2.tripplanning.destination.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${feign.user-service.url}")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    UserDTO getUser(@PathVariable("userId") Long userId);
}
