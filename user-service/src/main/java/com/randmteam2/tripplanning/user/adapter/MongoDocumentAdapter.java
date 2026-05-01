package com.randmteam2.tripplanning.user.adapter;

import com.randmteam2.tripplanning.user.model.AuthEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MongoDocumentAdapter {

    public Map<String, Object> adapt(AuthEvent event) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", event.getAction());
        result.put("timestamp", event.getTimestamp());
        result.put("details", event.getDetails() != null ? event.getDetails() : Map.of());
        return result;
    }
}
