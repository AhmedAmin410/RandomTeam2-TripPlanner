package com.randomteam2.tripplanning.destination.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DestinationRabbitMqConfig {

    public static final String DESTINATION_EVENTS_EXCHANGE = "destination.events";
    public static final String ITINERARY_EVENTS_EXCHANGE = "itinerary.events";
    public static final String ITINERARY_SAGA_QUEUE = "destination.itinerary.saga-listener";
    public static final String ITINERARY_SAGA_DLQ = "destination.itinerary.saga-listener.dlq";
    public static final String ITINERARY_SAGA_DLX = "destination.itinerary.saga-listener.dlx";

    @Bean
    public TopicExchange destinationEventsExchange() {
        return new TopicExchange(DESTINATION_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange itineraryEventsExchange() {
        return new TopicExchange(ITINERARY_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange itinerarySagaDeadLetterExchange() {
        return new DirectExchange(ITINERARY_SAGA_DLX, true, false);
    }

    @Bean
    public Queue itinerarySagaQueue() {
        return QueueBuilder.durable(ITINERARY_SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange", ITINERARY_SAGA_DLX)
                .withArgument("x-dead-letter-routing-key", ITINERARY_SAGA_DLQ)
                .build();
    }

    @Bean
    public Queue itinerarySagaDeadLetterQueue() {
        return QueueBuilder.durable(ITINERARY_SAGA_DLQ).build();
    }

    @Bean
    public Binding itineraryPlacedBinding(Queue itinerarySagaQueue, TopicExchange itineraryEventsExchange) {
        return BindingBuilder.bind(itinerarySagaQueue)
                .to(itineraryEventsExchange)
                .with("itinerary.placed");
    }

    @Bean
    public Binding itineraryCompletedBinding(Queue itinerarySagaQueue, TopicExchange itineraryEventsExchange) {
        return BindingBuilder.bind(itinerarySagaQueue)
                .to(itineraryEventsExchange)
                .with("itinerary.completed");
    }

    @Bean
    public Binding itineraryCancelledBinding(Queue itinerarySagaQueue, TopicExchange itineraryEventsExchange) {
        return BindingBuilder.bind(itinerarySagaQueue)
                .to(itineraryEventsExchange)
                .with("itinerary.cancelled");
    }

    @Bean
    public Binding itinerarySagaDlqBinding(Queue itinerarySagaDeadLetterQueue,
                                         DirectExchange itinerarySagaDeadLetterExchange) {
        return BindingBuilder.bind(itinerarySagaDeadLetterQueue)
                .to(itinerarySagaDeadLetterExchange)
                .with(ITINERARY_SAGA_DLQ);
    }

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
