package com.randmteam2.tripplanning.activity.messaging;

import com.randmteam2.tripplanning.activity.config.ActivityRabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Consumes upstream itinerary.* and destination.status-changed events (M3 §2.9)
 * and invalidates the S4-F10 activity-analytics cache so the next read recomputes
 * (idempotency Approach 1 — recompute from source-of-truth, replay is harmless).
 *
 * A dead-letter queue is wired in {@link ActivityRabbitMqConfig}; with
 * default-requeue-rejected=false and retry max-attempts=3, an exhausted message
 * is routed to the DLQ automatically — no manual ack/nack here.
 */
@Component
public class ActivitySagaListener {

    private static final Logger log = LoggerFactory.getLogger(ActivitySagaListener.class);

    private final CacheManager cacheManager;

    public ActivitySagaListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @RabbitListener(queues = ActivityRabbitMqConfig.SAGA_QUEUE)
    public void consume(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        Cache analytics = cacheManager.getCache("activity-service::S4-F10");
        if (analytics != null) {
            analytics.clear();
        }
        log.info("Invalidated S4-F10 analytics cache after {}", routingKey);
    }
}
