package com.randmteam2.tripplanning.itinerary.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ItineraryEventConfig {

    // exchanges
    @Bean
    public TopicExchange itineraryEventsExchange() {
        return new TopicExchange("itinerary.events");
    }

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange("user.events");
    }

    @Bean
    public TopicExchange destinationEventsExchange() {
        return new TopicExchange("destination.events");
    }

    @Bean
    public TopicExchange activityEventsExchange() {
        return new TopicExchange("activity.events");
    }

    @Bean
    public TopicExchange paymentEventsExchange() {
        return new TopicExchange("payment.events");
    }

    // queue 1: cache invalidation (user + destination + activity events)
    @Bean
    public Queue userEventsListenerQueue() {
        return QueueBuilder.durable("itinerary.user-events-listener")
                .withArgument("x-dead-letter-exchange", "itinerary.user-events-listener.dlx")
                .withArgument("x-dead-letter-routing-key", "itinerary.user-events-listener.dlq")
                .build();
    }

    @Bean
    public Queue userEventsListenerDLQ() {
        return QueueBuilder.durable("itinerary.user-events-listener.dlq").build();
    }

    @Bean
    public DirectExchange userEventsListenerDLX() {
        return new DirectExchange("itinerary.user-events-listener.dlx");
    }

    @Bean
    public Binding userEventsListenerDLQBinding() {
        return BindingBuilder.bind(userEventsListenerDLQ())
                .to(userEventsListenerDLX())
                .with("itinerary.user-events-listener.dlq");
    }

    @Bean
    public Binding bindUserRegistered() {
        return BindingBuilder.bind(userEventsListenerQueue())
                .to(userEventsExchange()).with("user.registered");
    }

    @Bean
    public Binding bindUserDeactivated() {
        return BindingBuilder.bind(userEventsListenerQueue())
                .to(userEventsExchange()).with("user.deactivated");
    }

    @Bean
    public Binding bindDestinationStatusChanged() {
        return BindingBuilder.bind(userEventsListenerQueue())
                .to(destinationEventsExchange()).with("destination.status-changed");
    }

    @Bean
    public Binding bindDestinationRated() {
        return BindingBuilder.bind(userEventsListenerQueue())
                .to(destinationEventsExchange()).with("destination.rated");
    }

    @Bean
    public Binding bindActivityCreated() {
        return BindingBuilder.bind(userEventsListenerQueue())
                .to(activityEventsExchange()).with("activity.created");
    }

    @Bean
    public Binding bindActivityLifecycleRecorded() {
        return BindingBuilder.bind(userEventsListenerQueue())
                .to(activityEventsExchange()).with("activity.lifecycle-recorded");
    }

    @Bean
    public Binding bindActivityCancelled() {
        return BindingBuilder.bind(userEventsListenerQueue())
                .to(activityEventsExchange()).with("activity.cancelled");
    }

    // queue 2: saga feedback (payment events)
    @Bean
    public Queue sagaFeedbackQueue() {
        return QueueBuilder.durable("itinerary.saga-feedback")
                .withArgument("x-dead-letter-exchange", "itinerary.saga-feedback.dlx")
                .withArgument("x-dead-letter-routing-key", "itinerary.saga-feedback.dlq")
                .build();
    }

    @Bean
    public Queue sagaFeedbackDLQ() {
        return QueueBuilder.durable("itinerary.saga-feedback.dlq").build();
    }

    @Bean
    public DirectExchange sagaFeedbackDLX() {
        return new DirectExchange("itinerary.saga-feedback.dlx");
    }

    @Bean
    public Binding sagaFeedbackDLQBinding() {
        return BindingBuilder.bind(sagaFeedbackDLQ())
                .to(sagaFeedbackDLX())
                .with("itinerary.saga-feedback.dlq");
    }

    @Bean
    public Binding bindPaymentInitiated() {
        return BindingBuilder.bind(sagaFeedbackQueue())
                .to(paymentEventsExchange()).with("payment.initiated");
    }

    @Bean
    public Binding bindPaymentCompleted() {
        return BindingBuilder.bind(sagaFeedbackQueue())
                .to(paymentEventsExchange()).with("payment.completed");
    }

    @Bean
    public Binding bindPaymentFailed() {
        return BindingBuilder.bind(sagaFeedbackQueue())
                .to(paymentEventsExchange()).with("payment.failed");
    }

    @Bean
    public Binding bindPaymentRefunded() {
        return BindingBuilder.bind(sagaFeedbackQueue())
                .to(paymentEventsExchange()).with("payment.refunded");
    }
}