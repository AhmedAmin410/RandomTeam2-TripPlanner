package com.randmteam2.tripplanning.user.mongo;

import com.randmteam2.tripplanning.user.model.AuthEvent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class EventFactory {

    private EventFactory() {}

    public static MongoEvent createEvent(EventType type, Map<String, Object> data) {
        if (type == null) {
            throw new IllegalArgumentException("EventType is required");
        }
        Map<String, Object> safe = data != null ? data : new HashMap<>();
        switch (type) {
            case AUTH:
                return buildAuth(safe);
            default:
                throw new IllegalArgumentException("Unsupported EventType: " + type);
        }
    }

    private static AuthEvent buildAuth(Map<String, Object> d) {
        AuthEvent ev = new AuthEvent();
        ev.setUserId(asLong(d.get("userId")));
        ev.setAction(asString(d.get("action")));
        ev.setTimestamp(asTimestamp(d.get("timestamp")));
        ev.setDetails(asDetails(d.get("details")));
        return ev;
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private static LocalDateTime asTimestamp(Object v) {
        if (v == null) return LocalDateTime.now();
        if (v instanceof LocalDateTime ldt) return ldt;
        try { return LocalDateTime.parse(v.toString()); }
        catch (Exception e) { return LocalDateTime.now(); }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asDetails(Object v) {
        if (v == null) return new HashMap<>();
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> out = new HashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }
        return new HashMap<>();
    }
}
