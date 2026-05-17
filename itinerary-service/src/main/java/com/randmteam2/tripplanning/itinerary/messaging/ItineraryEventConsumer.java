package com.randmteam2.tripplanning.itinerary.messaging;

import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Component
public class ItineraryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ItineraryEventConsumer.class);

    private final ItineraryRepository itineraryRepository;
    private final CacheManager cacheManager;
    private final ItineraryEventPublisher eventPublisher;

    public ItineraryEventConsumer(ItineraryRepository itineraryRepository,
                                  CacheManager cacheManager,
                                  ItineraryEventPublisher eventPublisher) {
        this.itineraryRepository = itineraryRepository;
        this.cacheManager = cacheManager;
        this.eventPublisher = eventPublisher;
    }

    // ── QUEUE 1: cache invalidation ──────────────────────────────────────────

    @RabbitListener(queues = "itinerary.user-events-listener")
    public void handleUserAndDestinationEvents(Map<String, Object> payload,
                                               org.springframework.amqp.core.Message message) {

        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.info("Received event [{}]: {}", routingKey, payload);

        switch (routingKey) {
            case "user.registered" -> { /* no-op */ }
            case "user.deactivated" -> {
                evictCache("itinerary-service::S3-F1");
                evictCache("itinerary-service::S3-F6");
            }
            case "destination.status-changed" -> {
                evictCache("itinerary-service::S3-F1");
                evictCache("itinerary-service::S3-F6");
            }
            case "destination.rated" ->
                    evictCache("itinerary-service::S3-F12");
            case "activity.created", "activity.lifecycle-recorded", "activity.cancelled" -> {
                Object itineraryId = payload.get("itineraryId");
                if (itineraryId != null) evictCache("itinerary-service::S3-F9");
            }
            default -> log.warn("Unhandled routing key: {}", routingKey);
        }
    }

    // ── QUEUE 2: saga feedback (payment events) ───────────────────────────────

    @RabbitListener(queues = "itinerary.saga-feedback")
    @Transactional
    public void handlePaymentEvents(Map<String, Object> payload,
                                    org.springframework.amqp.core.Message message) {

        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        Long itineraryId = getLong(payload, "itineraryId");
        log.info("Received saga event [{}] for itineraryId={}", routingKey, itineraryId);

        if (itineraryId == null) {
            log.warn("Missing itineraryId in payload, skipping");
            return;
        }

        switch (routingKey) {
            case "payment.initiated" ->
                    atomicTransition(itineraryId, Itinerary.Status.PAYMENT_PENDING, Itinerary.Status.COMPLETING);
            case "payment.completed" ->
                    atomicTransition(itineraryId, Itinerary.Status.PAID, Itinerary.Status.PAYMENT_PENDING);
            case "payment.failed" -> {
                int rows = atomicTransition(itineraryId, Itinerary.Status.PAYMENT_FAILED, Itinerary.Status.PAYMENT_PENDING);
                if (rows == 1) {
                    // compensation: publish itinerary.cancelled
                    itineraryRepository.findById(itineraryId).ifPresent(it ->
                            eventPublisher.publishItineraryCancelled(
                                    it.getId(), it.getUserId(), it.getDestinationId(), "payment_failed")
                    );
                }
            }
            case "payment.refunded" ->
                    atomicTransition(itineraryId, Itinerary.Status.REFUNDED, Itinerary.Status.PAYMENT_FAILED);
            default -> log.warn("Unhandled payment routing key: {}", routingKey);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private int atomicTransition(Long id, Itinerary.Status newStatus, Itinerary.Status expectedStatus) {
        int rows = itineraryRepository.atomicTransition(id, newStatus, expectedStatus);
        if (rows == 0) log.info("atomicTransition no-op: itinerary={} not in status {}", id, expectedStatus);
        else log.info("atomicTransition: itinerary={} → {}", id, newStatus);
        return rows;
    }

    private void evictCache(String cacheName) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) cache.clear();
        } catch (Exception e) {
            log.warn("Cache eviction failed for {}: {}", cacheName, e.getMessage());
        }
    }

    private Long getLong(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}