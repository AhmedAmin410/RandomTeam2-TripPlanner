package com.randomteam2.tripplanning.destination.mongo;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventFactory {

    public MongoEvent createEvent(EventType type, Map<String, Object> params) {
        if (type != EventType.DESTINATION) {
            throw new IllegalArgumentException("Unsupported event type for destination-service: " + type);
        }

        Map<String, Object> safeParams = params == null ? new HashMap<>() : new HashMap<>(params);

        Long destinationId = extractLong(safeParams.get("destinationId"));
        String action = String.valueOf(safeParams.getOrDefault("action", "UNKNOWN"));
        LocalDateTime timestamp = extractTimestamp(safeParams.get("timestamp"));

        Map<String, Object> details = new HashMap<>(safeParams);
        details.remove("destinationId");
        details.remove("action");
        details.remove("timestamp");

        return new DestinationEvent(destinationId, action, timestamp, details);
    }

    private Long extractLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.valueOf(value.toString());
    }

    private LocalDateTime extractTimestamp(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        return LocalDateTime.now();
    }
}