package com.randmteam2.tripplanning.user.listener;

import com.randmteam2.tripplanning.user.service.UserCacheInvalidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ItineraryEventListener {

    private static final Logger log = LoggerFactory.getLogger(ItineraryEventListener.class);

    private final UserCacheInvalidationService cacheInvalidationService;

    public ItineraryEventListener(UserCacheInvalidationService cacheInvalidationService) {
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @RabbitListener(queues = "user.itinerary.saga-listener")
    public void handleItineraryEvent(Map<String, Object> payload,
                                     @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        log.info("Received event routingKey={} payload={}", routingKey, payload);
        Long userId = extractUserId(payload);
        if (userId == null) {
            log.warn("No userId in event payload, skipping cache invalidation");
            return;
        }
        cacheInvalidationService.evictItineraryCaches(userId);
        log.info("Cache invalidated for userId={} after event={}", userId, routingKey);
    }

    private Long extractUserId(Map<String, Object> payload) {
        Object raw = payload.get("userId");
        if (raw instanceof Number n) return n.longValue();
        return null;
    }
}
