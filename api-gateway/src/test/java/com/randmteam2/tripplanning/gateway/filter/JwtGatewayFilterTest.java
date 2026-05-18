package com.randmteam2.tripplanning.gateway.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtGatewayFilterTest {

    @Test
    void bypassesAuthHealthActuatorAndPublicUserPaths() {
        assertTrue(JwtGatewayFilter.shouldBypassJwt("/api/auth/login"));
        assertTrue(JwtGatewayFilter.shouldBypassJwt("/api/auth/register"));
        assertTrue(JwtGatewayFilter.shouldBypassJwt("/api/users/register"));
        assertTrue(JwtGatewayFilter.shouldBypassJwt("/api/users/login"));
        assertTrue(JwtGatewayFilter.shouldBypassJwt("/api/users/health"));
        assertTrue(JwtGatewayFilter.shouldBypassJwt("/api/destinations/health"));
        assertTrue(JwtGatewayFilter.shouldBypassJwt("/api/itineraries/health"));
        assertTrue(JwtGatewayFilter.shouldBypassJwt("/api/activities/health"));
        assertTrue(JwtGatewayFilter.shouldBypassJwt("/api/bookings/health"));
        assertTrue(JwtGatewayFilter.shouldBypassJwt("/actuator/health"));
        assertTrue(JwtGatewayFilter.shouldBypassJwt("/actuator/health/liveness"));
        assertTrue(JwtGatewayFilter.shouldBypassJwt("/actuator/prometheus"));
    }

    @Test
    void requireJwtForProtectedApiPaths() {
        assertFalse(JwtGatewayFilter.shouldBypassJwt("/api/users/1"));
        assertFalse(JwtGatewayFilter.shouldBypassJwt("/api/destinations/1"));
    }
}
