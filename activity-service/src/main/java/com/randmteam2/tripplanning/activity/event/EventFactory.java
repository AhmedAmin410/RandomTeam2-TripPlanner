package com.randmteam2.tripplanning.activity.event;
import java.util.Map;
public class EventFactory {
    private EventFactory() {}
    public static MongoEvent createEvent(EventType type, Map<String, Object> data) {
        return switch (type) {
            case ACTIVITY -> {
                Long id = data.get("activityId") != null ? Long.parseLong(data.get("activityId").toString()) : null;
                yield new ActivityEvent(id, (String) data.getOrDefault("action","UNKNOWN"), (String) data.getOrDefault("details",""));
            }
            default -> throw new IllegalArgumentException("EventType " + type + " not handled in activity-service");
        };
    }
}
