package com.randmteam2.tripplanning.itinerary.events;

import com.randmteam2.tripplanning.itinerary.config.ItineraryRabbitMQConfig;
import com.randmteam2.tripplanning.itinerary.service.ItineraryService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ItineraryRabbitEventListener {

    private final ItineraryService itineraryService;

    public ItineraryRabbitEventListener(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @RabbitListener(queues = ItineraryRabbitMQConfig.ITINERARY_SERVICE_QUEUE)
    public void handleItineraryServiceEvents(
            Map<String, Object> message,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey
    ) {
        Long userId = extractLong(message.get("userId"));

        if ("user.registered".equals(routingKey)) {
            itineraryService.handleUserRegistered(userId);

            System.out.println("[RabbitMQ] user.registered consumed by itinerary-service for userId="
                    + userId);

            return;
        }

        if ("user.deactivated".equals(routingKey)) {
            itineraryService.handleUserDeactivated(userId);

            System.out.println("[RabbitMQ] user.deactivated consumed by itinerary-service for userId="
                    + userId);
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