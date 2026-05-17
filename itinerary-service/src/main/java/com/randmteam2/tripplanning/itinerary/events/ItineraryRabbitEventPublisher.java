package com.randmteam2.tripplanning.itinerary.events;

import com.randmteam2.tripplanning.itinerary.config.ItineraryRabbitMQConfig;
import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class ItineraryRabbitEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ItineraryRabbitEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishItineraryCompleted(Itinerary itinerary) {
        ItineraryEventMessage message = buildItineraryMessage("ITINERARY_COMPLETED", itinerary);

        rabbitTemplate.convertAndSend(
                ItineraryRabbitMQConfig.USER_EVENTS_EXCHANGE,
                "itinerary.completed",
                message
        );

        publishUserTripSummaryInvalidation(itinerary.getUserId(), "itinerary.completed");
    }

    public void publishItineraryCancelled(Itinerary itinerary) {
        ItineraryEventMessage message = buildItineraryMessage("ITINERARY_CANCELLED", itinerary);

        rabbitTemplate.convertAndSend(
                ItineraryRabbitMQConfig.USER_EVENTS_EXCHANGE,
                "itinerary.cancelled",
                message
        );

        publishUserTripSummaryInvalidation(itinerary.getUserId(), "itinerary.cancelled");
    }

    private void publishUserTripSummaryInvalidation(Long userId, String reason) {
        String cachePattern = "user-service::S1-F3::" + userId + "*";

        ItineraryEventMessage message = new ItineraryEventMessage(
                "CACHE_INVALIDATION",
                "itinerary-service",
                userId,
                null,
                null,
                cachePattern,
                LocalDateTime.now(),
                Map.of(
                        "cacheName", "S1-F3",
                        "reason", reason
                )
        );

        rabbitTemplate.convertAndSend(
                ItineraryRabbitMQConfig.USER_EVENTS_EXCHANGE,
                "invalidate.user-service.S1-F3." + userId + ".all",
                message
        );
    }

    private ItineraryEventMessage buildItineraryMessage(String eventType, Itinerary itinerary) {
        return new ItineraryEventMessage(
                eventType,
                "itinerary-service",
                itinerary.getUserId(),
                itinerary.getId(),
                itinerary.getStatus() != null ? itinerary.getStatus().name() : null,
                null,
                LocalDateTime.now(),
                Map.of(
                        "title", itinerary.getTitle(),
                        "estimatedBudget", itinerary.getEstimatedBudget() != null
                                ? itinerary.getEstimatedBudget()
                                : 0.0
                )
        );
    }
}