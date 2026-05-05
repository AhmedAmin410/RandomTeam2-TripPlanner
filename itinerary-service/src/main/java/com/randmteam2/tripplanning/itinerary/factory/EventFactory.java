package com.randmteam2.tripplanning.itinerary.factory;

import com.randmteam2.tripplanning.itinerary.mongo.ItineraryEvent;
import com.randmteam2.tripplanning.itinerary.mongo.MongoEvent;

import java.time.LocalDateTime;
import java.util.Map;

public class EventFactory {

    public static MongoEvent createEvent(EventType type, Map<String, Object> params) {
        return switch (type) {
            case ITINERARY -> {
                Long itineraryId = params.get("itineraryId") != null
                        ? Long.valueOf(params.get("itineraryId").toString())
                        : null;
                String action = (String) params.getOrDefault("action", "UNKNOWN");
                yield new ItineraryEvent(itineraryId, action, LocalDateTime.now(), params);
            }
            default -> throw new IllegalArgumentException("Unsupported EventType: " + type);
        };
    }
}
