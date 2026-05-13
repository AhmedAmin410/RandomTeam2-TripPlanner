package com.randmteam2.tripplanning.booking.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.randmteam2.tripplanning.booking.config.PaymentEventConfig;
import com.randmteam2.tripplanning.booking.service.SettlementService;
import com.randmteam2.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.randmteam2.tripplanning.contracts.events.ItineraryCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ItinerarySagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ItinerarySagaConsumer.class);

    private final SettlementService settlementService;
    private final ObjectMapper objectMapper;

    public ItinerarySagaConsumer(SettlementService settlementService, ObjectMapper objectMapper) {
        this.settlementService = settlementService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = PaymentEventConfig.PAYMENT_SAGA_QUEUE)
    public void consume(Message message) throws Exception {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String correlationId = (String) message.getMessageProperties().getHeaders().get("correlationId");
        try {
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }
            MDC.put("routingKey", routingKey);
            log.info("Consuming {} from {}", routingKey, PaymentEventConfig.PAYMENT_SAGA_QUEUE);

            if ("itinerary.completed".equals(routingKey)) {
                ItineraryCompletedEvent event = objectMapper.readValue(
                        message.getBody(), ItineraryCompletedEvent.class);
                MDC.put("itineraryId", String.valueOf(event.itineraryId()));
                MDC.put("userId", String.valueOf(event.userId()));
                settlementService.handleItineraryCompleted(event);
                log.info("Processed itinerary.completed for itinerary={}", event.itineraryId());
                return;
            }

            if ("itinerary.cancelled".equals(routingKey)) {
                ItineraryCancelledEvent event = objectMapper.readValue(
                        message.getBody(), ItineraryCancelledEvent.class);
                MDC.put("itineraryId", String.valueOf(event.itineraryId()));
                MDC.put("userId", String.valueOf(event.userId()));
                settlementService.handleItineraryCancelled(event);
                log.info("Processed itinerary.cancelled for itinerary={}", event.itineraryId());
                return;
            }

            log.warn("Ignoring unsupported routing key {}", routingKey);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("routingKey");
            MDC.remove("itineraryId");
            MDC.remove("userId");
        }
    }
}
