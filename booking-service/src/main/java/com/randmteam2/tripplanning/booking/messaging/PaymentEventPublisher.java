package com.randmteam2.tripplanning.booking.messaging;

import com.randmteam2.tripplanning.booking.config.PaymentEventConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(String routingKey, Object payload) {
        String correlationId = MDC.get("correlationId");
        rabbitTemplate.convertAndSend(PaymentEventConfig.PAYMENT_EXCHANGE, routingKey, payload, message -> {
            message.getMessageProperties().setHeader("routingKey", routingKey);
            if (correlationId != null) {
                message.getMessageProperties().setHeader("correlationId", correlationId);
            }
            return message;
        });
        log.info("Published {} with payload={}", routingKey, payload);
    }
}
