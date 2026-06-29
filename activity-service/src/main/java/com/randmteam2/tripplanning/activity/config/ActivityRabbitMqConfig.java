package com.randmteam2.tripplanning.activity.config;

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

/**
 * RabbitMQ topology for activity-service (M3 §2.9).
 *
 * Producer: declares the {@code activity.events} TopicExchange it publishes to
 *   (activity.created / activity.lifecycle-recorded / activity.cancelled).
 * Consumer: declares the saga queue + DLQ and binds it to the itinerary.events
 *   and destination.events exchanges so it reacts to upstream saga progression.
 */
@Configuration
public class ActivityRabbitMqConfig {

    public static final String ACTIVITY_EVENTS_EXCHANGE = "activity.events";
    public static final String ITINERARY_EVENTS_EXCHANGE = "itinerary.events";
    public static final String DESTINATION_EVENTS_EXCHANGE = "destination.events";

    public static final String SAGA_QUEUE = "activity.saga-listener";
    public static final String SAGA_DLQ = "activity.saga-listener.dlq";
    public static final String SAGA_DLX = "activity.saga-listener.dlx";

    // ── Producer exchange ──────────────────────────────────────────────────
    @Bean
    public TopicExchange activityEventsExchange() {
        return new TopicExchange(ACTIVITY_EVENTS_EXCHANGE, true, false);
    }

    // ── Consumer exchanges (same names — Spring deduplicates) ──────────────
    @Bean
    public TopicExchange itineraryEventsExchange() {
        return new TopicExchange(ITINERARY_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange destinationEventsExchange() {
        return new TopicExchange(DESTINATION_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange activitySagaDeadLetterExchange() {
        return new DirectExchange(SAGA_DLX, true, false);
    }

    @Bean
    public Queue activitySagaQueue() {
        return QueueBuilder.durable(SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange", SAGA_DLX)
                .withArgument("x-dead-letter-routing-key", SAGA_DLQ)
                .build();
    }

    @Bean
    public Queue activitySagaDeadLetterQueue() {
        return QueueBuilder.durable(SAGA_DLQ).build();
    }

    @Bean
    public Binding activityItineraryPlacedBinding(Queue activitySagaQueue, TopicExchange itineraryEventsExchange) {
        return BindingBuilder.bind(activitySagaQueue).to(itineraryEventsExchange).with("itinerary.placed");
    }

    @Bean
    public Binding activityItineraryCompletedBinding(Queue activitySagaQueue, TopicExchange itineraryEventsExchange) {
        return BindingBuilder.bind(activitySagaQueue).to(itineraryEventsExchange).with("itinerary.completed");
    }

    @Bean
    public Binding activityItineraryCancelledBinding(Queue activitySagaQueue, TopicExchange itineraryEventsExchange) {
        return BindingBuilder.bind(activitySagaQueue).to(itineraryEventsExchange).with("itinerary.cancelled");
    }

    @Bean
    public Binding activityDestinationStatusChangedBinding(Queue activitySagaQueue, TopicExchange destinationEventsExchange) {
        return BindingBuilder.bind(activitySagaQueue).to(destinationEventsExchange).with("destination.status-changed");
    }

    @Bean
    public Binding activitySagaDlqBinding(Queue activitySagaDeadLetterQueue, DirectExchange activitySagaDeadLetterExchange) {
        return BindingBuilder.bind(activitySagaDeadLetterQueue).to(activitySagaDeadLetterExchange).with(SAGA_DLQ);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
