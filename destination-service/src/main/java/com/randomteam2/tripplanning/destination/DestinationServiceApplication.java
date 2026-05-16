package com.randomteam2.tripplanning.destination;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.randomteam2.tripplanning.destination.feign")
public class DestinationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DestinationServiceApplication.class, args);
    }

}
