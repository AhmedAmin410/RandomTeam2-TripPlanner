package com.randomteam2.tripplanning.destination.messaging;

import com.randomteam2.tripplanning.destination.config.DestinationRabbitMqConfig;
import com.randomteam2.tripplanning.destination.dto.DestinationRatedEvent;
import com.randomteam2.tripplanning.destination.dto.StatusChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DestinationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public DestinationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishStatusChanged(StatusChangedEvent event) {
        rabbitTemplate.convertAndSend(
                DestinationRabbitMqConfig.DESTINATION_EVENTS_EXCHANGE,
                "destination.status-changed",
                event);
    }

    public void publishRated(DestinationRatedEvent event) {
        rabbitTemplate.convertAndSend(
                DestinationRabbitMqConfig.DESTINATION_EVENTS_EXCHANGE,
                "destination.rated",
                event);
    }
}
