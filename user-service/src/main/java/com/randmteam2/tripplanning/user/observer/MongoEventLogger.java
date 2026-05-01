package com.randmteam2.tripplanning.user.observer;

import com.randmteam2.tripplanning.user.model.AuthEvent;
import com.randmteam2.tripplanning.user.mongo.EventFactory;
import com.randmteam2.tripplanning.user.mongo.EventType;
import com.randmteam2.tripplanning.user.mongo.MongoEvent;
import com.randmteam2.tripplanning.user.repository.AuthEventRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {

    private final AuthEventRepository authEventRepository;

    public MongoEventLogger(AuthEventRepository authEventRepository) {
        this.authEventRepository = authEventRepository;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("action", eventType);
            data.put("timestamp", LocalDateTime.now());

            if (payload instanceof Map<?, ?> m) {
                Object userId = m.get("userId");
                if (userId != null) {
                    data.put("userId", userId);
                }
                Map<String, Object> details = new HashMap<>();
                for (Map.Entry<?, ?> entry : m.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    if (!"userId".equals(key)) {
                        details.put(key, entry.getValue());
                    }
                }
                data.put("details", details);
            }

            MongoEvent event = EventFactory.createEvent(EventType.AUTH, data);
            authEventRepository.save((AuthEvent) event);
        } catch (Exception e) {
            System.err.println("[WARN] MongoDB event write failed for " + eventType + ": " + e.getMessage());
        }
    }
}
