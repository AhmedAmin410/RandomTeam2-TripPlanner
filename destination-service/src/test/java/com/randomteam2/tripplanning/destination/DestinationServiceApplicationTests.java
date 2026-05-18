package com.randomteam2.tripplanning.destination;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.EnableFeignClients;

import static org.assertj.core.api.Assertions.assertThat;

class DestinationServiceApplicationTests {

    @Test
    void applicationEnablesFeignClients() {
        assertThat(DestinationServiceApplication.class.isAnnotationPresent(EnableFeignClients.class))
                .isTrue();
    }
}
