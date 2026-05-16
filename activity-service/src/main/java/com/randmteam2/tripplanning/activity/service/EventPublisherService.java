package com.randmteam2.tripplanning.activity.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    public EventPublisherService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishActivityCreated(String routingKey, Object message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    public void publishActivityLifecycleRecorded(String routingKey, Object message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    public void publishActivityCancelled(String routingKey, Object message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}