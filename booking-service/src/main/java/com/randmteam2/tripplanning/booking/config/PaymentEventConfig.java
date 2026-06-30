package com.randmteam2.tripplanning.booking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentEventConfig {

    // ── Exchange names ────────────────────────────────────────────────────
    public static final String PAYMENT_EXCHANGE   = "payment.events";
    public static final String ITINERARY_EXCHANGE = "itinerary.events";

    // ── Queue names ───────────────────────────────────────────────────────
    public static final String PAYMENT_SAGA_QUEUE = "payment.saga-listener";
    public static final String PAYMENT_SAGA_DLQ   = "payment.saga-listener.dlq";
    public static final String PAYMENT_SAGA_DLX   = "payment.saga-listener.dlx";

    // ── Exchanges ─────────────────────────────────────────────────────────

    /** Producer: booking-service publishes payment.* events here */
    @Bean
    public TopicExchange paymentEventsExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    /** Consumer reference: itinerary.events is declared by itinerary-service;
     *  booking-service declares a reference so Spring deduplicates safely. */
    @Bean
    public TopicExchange itineraryEventsExchange() {
        return new TopicExchange(ITINERARY_EXCHANGE, true, false);
    }

    // ── Dead-letter exchange + DLQ ────────────────────────────────────────

    @Bean
    public DirectExchange paymentSagaDeadLetterExchange() {
        return new DirectExchange(PAYMENT_SAGA_DLX, true, false);
    }

    @Bean
    public Queue paymentSagaDeadLetterQueue() {
        return QueueBuilder.durable(PAYMENT_SAGA_DLQ).build();
    }

    @Bean
    public Binding paymentSagaDlqBinding(Queue paymentSagaDeadLetterQueue,
                                         DirectExchange paymentSagaDeadLetterExchange) {
        return BindingBuilder.bind(paymentSagaDeadLetterQueue)
                .to(paymentSagaDeadLetterExchange)
                .with(PAYMENT_SAGA_DLQ);
    }

    // ── Consumer queue (binds itinerary.completed + itinerary.cancelled) ──

    @Bean
    public Queue paymentSagaQueue() {
        return QueueBuilder.durable(PAYMENT_SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange",    PAYMENT_SAGA_DLX)
                .withArgument("x-dead-letter-routing-key", PAYMENT_SAGA_DLQ)
                .build();
    }

    @Bean
    public Binding completedBinding(Queue paymentSagaQueue,
                                    TopicExchange itineraryEventsExchange) {
        return BindingBuilder.bind(paymentSagaQueue)
                .to(itineraryEventsExchange)
                .with("itinerary.completed");
    }

    @Bean
    public Binding cancelledBinding(Queue paymentSagaQueue,
                                    TopicExchange itineraryEventsExchange) {
        return BindingBuilder.bind(paymentSagaQueue)
                .to(itineraryEventsExchange)
                .with("itinerary.cancelled");
    }

    // ── JSON message converter ────────────────────────────────────────────

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonMessageConverter);
        return rabbitTemplate;
    }
}
