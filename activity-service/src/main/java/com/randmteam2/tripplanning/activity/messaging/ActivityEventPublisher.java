package com.randmteam2.tripplanning.activity.messaging;

import com.randmteam2.tripplanning.activity.config.ActivityRabbitMqConfig;
import com.randmteam2.tripplanning.contracts.events.ActivityCancelledEvent;
import com.randmteam2.tripplanning.contracts.events.ActivityCreatedEvent;
import com.randmteam2.tripplanning.contracts.events.ActivityLifecycleRecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publishes activity-service domain events to the {@code activity.events}
 * TopicExchange (M3 §2.9). RabbitMQ is a soft dependency — a broker failure is
 * logged and swallowed so the primary PostgreSQL/Cassandra write is unaffected.
 */
@Component
public class ActivityEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ActivityEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ActivityEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishActivityCreated(ActivityCreatedEvent event) {
        send("activity.created", Map.of(
                "eventType", "activity.created",
                "activityId", event.activityId(),
                "itineraryId", event.itineraryId(),
                "category", event.category()
        ), event.activityId());
    }

    public void publishLifecycleRecorded(ActivityLifecycleRecordedEvent event) {
        send("activity.lifecycle-recorded", Map.of(
                "eventType", "activity.lifecycle-recorded",
                "activityId", event.activityId(),
                "itineraryId", event.itineraryId(),
                "status", event.status()
        ), event.activityId());
    }

    public void publishActivityCancelled(ActivityCancelledEvent event) {
        send("activity.cancelled", Map.of(
                "eventType", "activity.cancelled",
                "activityId", event.activityId(),
                "itineraryId", event.itineraryId()
        ), event.activityId());
    }

    private void send(String routingKey, Object event, Long activityId) {
        try {
            rabbitTemplate.convertAndSend(ActivityRabbitMqConfig.ACTIVITY_EVENTS_EXCHANGE, routingKey, event);
            log.info("Published {} for activityId={}", routingKey, activityId);
        } catch (Exception e) {
            log.error("Failed to publish {} for activityId={}", routingKey, activityId, e);
        }
    }
}
