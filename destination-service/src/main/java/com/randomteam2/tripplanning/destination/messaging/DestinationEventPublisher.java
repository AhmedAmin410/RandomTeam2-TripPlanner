package com.randomteam2.tripplanning.destination.messaging;

import com.randomteam2.tripplanning.destination.config.DestinationRabbitMqConfig;
import com.randomteam2.tripplanning.destination.dto.DestinationRatedEvent;
import com.randomteam2.tripplanning.destination.dto.StatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DestinationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DestinationEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public DestinationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishStatusChanged(StatusChangedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    DestinationRabbitMqConfig.DESTINATION_EVENTS_EXCHANGE,
                    "destination.status-changed",
                    Map.of(
                            "eventType", "destination.status-changed",
                            "destinationId", event.destinationId(),
                            "oldStatus", event.oldStatus(),
                            "newStatus", event.newStatus()
                    ));
            log.info("Published destination.status-changed for destinationId={} ({} -> {})",
                    event.destinationId(), event.oldStatus(), event.newStatus());
        } catch (Exception e) {
            log.error("Failed to publish destination.status-changed for destinationId={}", event.destinationId(), e);
        }
    }

    public void publishRated(DestinationRatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    DestinationRabbitMqConfig.DESTINATION_EVENTS_EXCHANGE,
                    "destination.rated",
                    Map.of(
                            "eventType", "destination.rated",
                            "destinationId", event.destinationId(),
                            "itineraryId", event.itineraryId(),
                            "rating", event.rating(),
                            "userId", event.userId()
                    ));
            log.info("Published destination.rated for destinationId={} itineraryId={} rating={}",
                    event.destinationId(), event.itineraryId(), event.rating());
        } catch (Exception e) {
            log.error("Failed to publish destination.rated for destinationId={}", event.destinationId(), e);
        }
    }
}
