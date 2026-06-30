package com.randmteam2.tripplanning.user.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqTopologyConfig {

    // Producer: user-service publishes to this exchange
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter());
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

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
