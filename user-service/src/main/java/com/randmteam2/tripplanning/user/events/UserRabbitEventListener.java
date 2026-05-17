package com.randmteam2.tripplanning.user.events;

import com.randmteam2.tripplanning.user.config.UserRabbitMQConfig;
import com.randmteam2.tripplanning.user.service.UserCacheInvalidationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserRabbitEventListener {

    private final UserCacheInvalidationService cacheInvalidationService;

    public UserRabbitEventListener(UserCacheInvalidationService cacheInvalidationService) {
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @RabbitListener(queues = UserRabbitMQConfig.USER_SERVICE_QUEUE)
    public void handleUserServiceEvents(
            Map<String, Object> message,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey
    ) {
        Long userId = extractLong(message.get("userId"));

        if ("itinerary.completed".equals(routingKey) ||
                "itinerary.cancelled".equals(routingKey)) {

            cacheInvalidationService.evictUserTripSummaryCache(userId);

            System.out.println("[RabbitMQ] S1-F3 cache invalidated for userId="
                    + userId + " because of " + routingKey);

            return;
        }

        if (routingKey != null && routingKey.startsWith("invalidate.user-service")) {
            Object pattern = message.get("cacheKeyPattern");

            if (pattern != null) {
                cacheInvalidationService.evictByPattern(pattern.toString());

                System.out.println("[RabbitMQ] Cache invalidated by pattern: "
                        + pattern);
            }
        }
    }

    private Long extractLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}