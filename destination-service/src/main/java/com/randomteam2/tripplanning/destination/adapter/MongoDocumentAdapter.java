package com.randomteam2.tripplanning.destination.adapter;

import com.randomteam2.tripplanning.destination.mongo.DestinationEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter Pattern: converts a MongoDB DestinationEvent document
 * into a generic Map DTO for API responses.
 */
@Component
public class MongoDocumentAdapter {

    public Map<String, Object> adapt(DestinationEvent event) {
        if (event == null) {
            return null;
        }
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", event.getId());
        dto.put("destinationId", event.getDestinationId());
        dto.put("action", event.getAction());
        dto.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : null);
        dto.put("details", event.getDetails());
        return dto;
    }
}
