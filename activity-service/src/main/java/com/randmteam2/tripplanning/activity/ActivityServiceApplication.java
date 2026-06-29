package com.randmteam2.tripplanning.activity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import com.randmteam2.tripplanning.contracts.feign.ItineraryServiceClient;

@SpringBootApplication
@EnableCaching
// Register only the Feign client activity-service actually uses. Scanning the
// whole contracts.feign package instantiates all four clients, but activity only
// configures feign.itinerary-service.url — the other three resolve to an
// unresolved ${...} URL and fail at startup ("Illegal character found in host: '{'").
@EnableFeignClients(clients = ItineraryServiceClient.class)
public class ActivityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
