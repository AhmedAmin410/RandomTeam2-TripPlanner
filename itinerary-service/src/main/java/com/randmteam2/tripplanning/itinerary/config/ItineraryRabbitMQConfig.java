package com.randmteam2.tripplanning.itinerary.config;

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
public class ItineraryRabbitMQConfig {

    public static final String USER_EVENTS_EXCHANGE = "user.events";

    public static final String ITINERARY_SERVICE_QUEUE = "itinerary-service.events.queue";
    public static final String ITINERARY_SERVICE_DLQ = "itinerary-service.events.dlq";

    public static final String ITINERARY_SERVICE_DLX = "itinerary-service.events.dlx";

    public static final String ROUTING_USER_REGISTERED = "user.registered";
    public static final String ROUTING_USER_DEACTIVATED = "user.deactivated";

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange itineraryServiceDeadLetterExchange() {
        return new TopicExchange(ITINERARY_SERVICE_DLX, true, false);
    }

    @Bean
    public Queue itineraryServiceQueue() {
        return QueueBuilder.durable(ITINERARY_SERVICE_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", ITINERARY_SERVICE_DLX,
                        "x-dead-letter-routing-key", "itinerary-service.failed"
                ))
                .build();
    }

    @Bean
    public Queue itineraryServiceDlq() {
        return QueueBuilder.durable(ITINERARY_SERVICE_DLQ).build();
    }

    @Bean
    public Binding bindUserRegistered(Queue itineraryServiceQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(itineraryServiceQueue)
                .to(userEventsExchange)
                .with(ROUTING_USER_REGISTERED);
    }

    @Bean
    public Binding bindUserDeactivated(Queue itineraryServiceQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(itineraryServiceQueue)
                .to(userEventsExchange)
                .with(ROUTING_USER_DEACTIVATED);
    }

    @Bean
    public Binding bindItineraryServiceDlq(Queue itineraryServiceDlq,
                                           TopicExchange itineraryServiceDeadLetterExchange) {
        return BindingBuilder.bind(itineraryServiceDlq)
                .to(itineraryServiceDeadLetterExchange)
                .with("itinerary-service.failed");
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