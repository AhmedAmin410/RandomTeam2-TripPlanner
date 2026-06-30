package com.randmteam2.tripplanning.booking.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.randmteam2.tripplanning.booking.config.PaymentEventConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String routingKey, Object payload) {
        String correlationId = MDC.get("correlationId");
        Map<String, Object> eventPayload = objectMapper.convertValue(
                payload,
                new TypeReference<LinkedHashMap<String, Object>>() {}
        );
        eventPayload.put("eventType", routingKey);

        rabbitTemplate.convertAndSend(PaymentEventConfig.PAYMENT_EXCHANGE, routingKey, eventPayload, message -> {
            message.getMessageProperties().setHeader("routingKey", routingKey);
            if (correlationId != null) {
                message.getMessageProperties().setHeader("correlationId", correlationId);
            }
            return message;
        });
        log.info("Published {} with payload={}", routingKey, eventPayload);
    }
}
