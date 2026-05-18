package com.randmteam2.tripplanning.itinerary.messaging;

import com.randmteam2.tripplanning.itinerary.repository.ItineraryRepository;
import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Component
public class ItineraryEventConsumer {

    private final ItineraryRepository itineraryRepository;
    private final CacheManager cacheManager;
    private final ItineraryEventPublisher itineraryEventPublisher;

    public ItineraryEventConsumer(ItineraryRepository itineraryRepository,
                                  CacheManager cacheManager,
                                  ItineraryEventPublisher itineraryEventPublisher) {
        this.itineraryRepository = itineraryRepository;
        this.cacheManager = cacheManager;
        this.itineraryEventPublisher = itineraryEventPublisher;
    }

    // ── CACHE INVALIDATION CONSUMERS ─────────────────────────────────────

    @RabbitListener(queues = "itinerary.user-events-listener")
    public void handleUserAndDestinationEvents(Map<String, Object> event) {
        String type = (String) event.get("eventType");
        if (type == null) return;

        switch (type) {
            case "user.registered" -> {
                // no-op
            }
            case "user.deactivated" -> {
                evict("itinerary-service::S3-F1");
                evict("itinerary-service::S3-F6");
            }
            case "destination.status-changed" -> {
                evict("itinerary-service::S3-F1");
                evict("itinerary-service::S3-F6");
            }
            case "destination.rated" -> {
                evict("itinerary-service::S3-F12");
            }
            case "activity.created", "activity.lifecycle-recorded", "activity.cancelled" -> {
                Object itineraryId = event.get("itineraryId");
                if (itineraryId != null) {
                    evictKey("itinerary-service::S3-F9", itineraryId.toString());
                }
            }
        }
    }

    // ── SAGA FEEDBACK CONSUMERS ───────────────────────────────────────────

    @RabbitListener(queues = "itinerary.saga-feedback")
    @Transactional
    public void handlePaymentEvents(Map<String, Object> event) {
        String type = (String) event.get("eventType");
        if (type == null) return;

        Long itineraryId = getLong(event, "itineraryId");
        if (itineraryId == null) return;

        switch (type) {
            case "payment.initiated" -> {
                int updated = itineraryRepository.atomicTransition(
                        itineraryId,
                        Itinerary.Status.PAYMENT_PENDING,
                        Itinerary.Status.COMPLETING
                );
                // rowcount=0 means duplicate — silent no-op
            }
            case "payment.completed" -> {
                itineraryRepository.atomicTransition(
                        itineraryId,
                        Itinerary.Status.PAID,
                        Itinerary.Status.PAYMENT_PENDING
                );
            }
            case "payment.failed" -> {
                int updated = itineraryRepository.atomicTransition(
                        itineraryId,
                        Itinerary.Status.PAYMENT_FAILED,
                        Itinerary.Status.PAYMENT_PENDING
                );
                if (updated == 1) {
                    // compensation — publish cancellation
                    itineraryRepository.findById(itineraryId).ifPresent(itinerary ->
                            itineraryEventPublisher.publishItineraryCancelled(
                                    itineraryId,
                                    itinerary.getUserId(),
                                    itinerary.getDestinationId(),
                                    "payment_failed"
                            )
                    );
                }
            }
            case "payment.refunded" -> {
                itineraryRepository.atomicTransition(
                        itineraryId,
                        Itinerary.Status.REFUNDED,
                        Itinerary.Status.PAYMENT_FAILED
                );
            }
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────

    private void evict(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.clear();
    }

    private void evictKey(String cacheName, String key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.evict(key);
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Long l) return l;
        if (val instanceof Integer i) return i.longValue();
        if (val instanceof String s) return Long.parseLong(s);
        return null;
    }
}