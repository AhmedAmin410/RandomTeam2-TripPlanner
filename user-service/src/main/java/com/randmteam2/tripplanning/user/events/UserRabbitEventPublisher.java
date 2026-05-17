package com.randmteam2.tripplanning.user.events;

import com.randmteam2.tripplanning.user.config.UserRabbitMQConfig;
import com.randmteam2.tripplanning.user.model.User;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class UserRabbitEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public UserRabbitEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUserRegistered(User user) {
        UserEventMessage message = new UserEventMessage(
                "USER_REGISTERED",
                "user-service",
                user.getId(),
                null,
                user.getStatus() != null ? user.getStatus().name() : null,
                null,
                LocalDateTime.now(),
                Map.of(
                        "email", user.getEmail(),
                        "name", user.getName()
                )
        );

        rabbitTemplate.convertAndSend(
                UserRabbitMQConfig.USER_EVENTS_EXCHANGE,
                "user.registered",
                message
        );
    }

    public void publishUserDeactivated(User user) {
        UserEventMessage message = new UserEventMessage(
                "USER_DEACTIVATED",
                "user-service",
                user.getId(),
                null,
                user.getStatus() != null ? user.getStatus().name() : null,
                null,
                LocalDateTime.now(),
                Map.of(
                        "email", user.getEmail(),
                        "name", user.getName()
                )
        );

        rabbitTemplate.convertAndSend(
                UserRabbitMQConfig.USER_EVENTS_EXCHANGE,
                "user.deactivated",
                message
        );
    }
}