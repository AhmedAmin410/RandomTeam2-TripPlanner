package com.randmteam2.tripplanning.user.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqTopologyConfig {

    // Producer: user-service publishes to this exchange
    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange("user.events", true, false);
    }

    // Consumer: user-service listens to itinerary.events
    @Bean
    public TopicExchange itineraryEventsExchange() {
        return new TopicExchange("itinerary.events", true, false);
    }

    @Bean
    public Queue userItinerarySagaListenerQueue() {
        return QueueBuilder.durable("user.itinerary.saga-listener")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "user.itinerary.saga-listener.dlq")
                .build();
    }

    @Bean
    public Queue userItinerarySagaListenerDlq() {
        return QueueBuilder.durable("user.itinerary.saga-listener.dlq").build();
    }

    @Bean
    public Binding itineraryCompletedBinding() {
        return BindingBuilder.bind(userItinerarySagaListenerQueue())
                .to(itineraryEventsExchange())
                .with("itinerary.completed");
    }

    @Bean
    public Binding itineraryCancelledBinding() {
        return BindingBuilder.bind(userItinerarySagaListenerQueue())
                .to(itineraryEventsExchange())
                .with("itinerary.cancelled");
    }
}
