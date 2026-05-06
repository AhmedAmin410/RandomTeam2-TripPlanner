package com.randmteam2.tripplanning.activity.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
