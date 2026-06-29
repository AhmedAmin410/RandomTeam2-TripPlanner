package com.randmteam2.tripplanning.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayActuatorSecurityIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void actuatorHealthOkWithoutJwt() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("application/vnd.spring-boot.actuator.v3+json");
    }

    @Test
    void protectedRouteUnauthorizedWithoutJwt() {
        webTestClient.get()
                .uri("/api/activities/ping")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
