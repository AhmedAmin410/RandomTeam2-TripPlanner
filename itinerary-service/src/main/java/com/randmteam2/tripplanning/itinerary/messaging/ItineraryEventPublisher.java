package com.randmteam2.tripplanning.itinerary.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ItineraryEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ItineraryEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishItineraryPlaced(Long itineraryId, Long userId, Long destinationId) {
        Map<String, Object> payload = Map.of(
                "itineraryId", itineraryId,
                "userId", userId,
                "destinationId", destinationId
        );
        rabbitTemplate.convertAndSend("itinerary.events", "itinerary.placed", payload);
    }

    public void publishItineraryCompleted(Long itineraryId, Long userId, Long destinationId, Double totalAmount) {
        Map<String, Object> payload = Map.of(
                "itineraryId", itineraryId,
                "userId", userId,
                "destinationId", destinationId,
                "totalAmount", totalAmount
        );
        rabbitTemplate.convertAndSend("itinerary.events", "itinerary.completed", payload);
    }

    public void publishItineraryCancelled(Long itineraryId, Long userId, Long destinationId, String reason) {
        Map<String, Object> payload = Map.of(
                "itineraryId", itineraryId,
                "userId", userId,
                "destinationId", destinationId,
                "reason", reason
        );
        rabbitTemplate.convertAndSend("itinerary.events", "itinerary.cancelled", payload);
    }
}