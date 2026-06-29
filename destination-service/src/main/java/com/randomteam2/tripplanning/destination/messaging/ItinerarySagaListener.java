package com.randomteam2.tripplanning.destination.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.randomteam2.tripplanning.destination.config.DestinationRabbitMqConfig;
import com.randomteam2.tripplanning.destination.service.DestinationCacheInvalidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ItinerarySagaListener {

    private static final Logger log = LoggerFactory.getLogger(ItinerarySagaListener.class);

    private final ObjectMapper objectMapper;
    private final DestinationCacheInvalidationService cacheInvalidationService;

    public ItinerarySagaListener(ObjectMapper objectMapper,
                                 DestinationCacheInvalidationService cacheInvalidationService) {
        this.objectMapper = objectMapper;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @RabbitListener(queues = DestinationRabbitMqConfig.ITINERARY_SAGA_QUEUE)
    public void consume(Message message) throws Exception {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        if (!isSupportedRoutingKey(routingKey)) {
            log.warn("Ignoring unsupported routing key {}", routingKey);
            return;
        }

        JsonNode payload = objectMapper.readTree(message.getBody());
        Long destinationId = extractDestinationId(payload);
        if (destinationId == null) {
            log.warn("No destinationId in {} payload; skipping cache invalidation", routingKey);
            return;
        }

        cacheInvalidationService.invalidateItinerarySagaCaches(destinationId);
        log.info("Invalidated S2-F3/S2-F12 caches for destination {} after {}", destinationId, routingKey);
    }

    private static boolean isSupportedRoutingKey(String routingKey) {
        return "itinerary.placed".equals(routingKey)
                || "itinerary.completed".equals(routingKey)
                || "itinerary.cancelled".equals(routingKey);
    }

    private static Long extractDestinationId(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return null;
        }
        JsonNode node = payload.get("destinationId");
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asLong();
    }
}
