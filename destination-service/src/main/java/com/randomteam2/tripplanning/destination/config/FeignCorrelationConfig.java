package com.randomteam2.tripplanning.destination.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignCorrelationConfig {

    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return template -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                template.header("X-Correlation-ID", correlationId);
            }
        };
    }

    /**
     * Forward the caller's Authorization header on outgoing Feign calls so the
     * downstream service authenticates the same principal. S2-F3/F7/F8/F12 all
     * fan out to itinerary-service / user-service, which require a valid JWT.
     */
    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            if (template.headers().containsKey("Authorization")) {
                return;
            }
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String auth = attrs.getRequest().getHeader("Authorization");
                if (auth != null && !auth.isBlank()) {
                    template.header("Authorization", auth);
                }
            }
        };
    }
}
