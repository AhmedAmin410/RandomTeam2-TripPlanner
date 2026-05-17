package com.randmteam2.tripplanning.user.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class UserRabbitMQConfig {

    public static final String USER_EVENTS_EXCHANGE = "user.events";

    public static final String USER_SERVICE_QUEUE = "user-service.events.queue";
    public static final String USER_SERVICE_DLQ = "user-service.events.dlq";

    public static final String USER_SERVICE_DLX = "user-service.events.dlx";

    public static final String ROUTING_ITINERARY_COMPLETED = "itinerary.completed";
    public static final String ROUTING_ITINERARY_CANCELLED = "itinerary.cancelled";
    public static final String ROUTING_INVALIDATE_USER_SERVICE = "invalidate.user-service.#";

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange userServiceDeadLetterExchange() {
        return new TopicExchange(USER_SERVICE_DLX, true, false);
    }

    @Bean
    public Queue userServiceQueue() {
        return QueueBuilder.durable(USER_SERVICE_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", USER_SERVICE_DLX,
                        "x-dead-letter-routing-key", "user-service.failed"
                ))
                .build();
    }

    @Bean
    public Queue userServiceDlq() {
        return QueueBuilder.durable(USER_SERVICE_DLQ).build();
    }

    @Bean
    public Binding bindItineraryCompleted(Queue userServiceQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(userServiceQueue)
                .to(userEventsExchange)
                .with(ROUTING_ITINERARY_COMPLETED);
    }

    @Bean
    public Binding bindItineraryCancelled(Queue userServiceQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(userServiceQueue)
                .to(userEventsExchange)
                .with(ROUTING_ITINERARY_CANCELLED);
    }

    @Bean
    public Binding bindUserCacheInvalidation(Queue userServiceQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(userServiceQueue)
                .to(userEventsExchange)
                .with(ROUTING_INVALIDATE_USER_SERVICE);
    }

    @Bean
    public Binding bindUserServiceDlq(Queue userServiceDlq, TopicExchange userServiceDeadLetterExchange) {
        return BindingBuilder.bind(userServiceDlq)
                .to(userServiceDeadLetterExchange)
                .with("user-service.failed");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}