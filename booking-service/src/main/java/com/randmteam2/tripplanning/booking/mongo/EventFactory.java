package com.randmteam2.tripplanning.booking.mongo;

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
            case AUTH:           return buildAuth(safe);
            case DESTINATION:    return buildDestination(safe);
            case ITINERARY:      return buildItinerary(safe);
            case ACTIVITY:       return buildActivity(safe);
            case PAYMENT_AUDIT:  return buildPaymentAudit(safe);
            default:
                throw new IllegalArgumentException("Unsupported EventType: " + type);
        }
    }

    private static AuthEvent buildAuth(Map<String, Object> d) {
        AuthEvent ev = new AuthEvent();
        ev.setAction(asString(d.get("action")));
        ev.setTimestamp(asTimestamp(d.get("timestamp")));
        ev.setUserId(asLong(d.get("userId")));
        ev.setUsername(asString(d.get("username")));
        ev.setIpAddress(asString(d.get("ipAddress")));
        ev.setDetails(asDetails(d.get("details")));
        return ev;
    }

    private static DestinationEvent buildDestination(Map<String, Object> d) {
        DestinationEvent ev = new DestinationEvent();
        ev.setAction(asString(d.get("action")));
        ev.setTimestamp(asTimestamp(d.get("timestamp")));
        ev.setDestinationId(asLong(d.get("destinationId")));
        ev.setDestinationName(asString(d.get("destinationName")));
        ev.setCountry(asString(d.get("country")));
        ev.setDetails(asDetails(d.get("details")));
        return ev;
    }

    private static ItineraryEvent buildItinerary(Map<String, Object> d) {
        ItineraryEvent ev = new ItineraryEvent();
        ev.setAction(asString(d.get("action")));
        ev.setTimestamp(asTimestamp(d.get("timestamp")));
        ev.setItineraryId(asLong(d.get("itineraryId")));
        ev.setUserId(asLong(d.get("userId")));
        ev.setStatus(asString(d.get("status")));
        ev.setDetails(asDetails(d.get("details")));
        return ev;
    }

    private static ActivityEvent buildActivity(Map<String, Object> d) {
        ActivityEvent ev = new ActivityEvent();
        ev.setAction(asString(d.get("action")));
        ev.setTimestamp(asTimestamp(d.get("timestamp")));
        ev.setActivityId(asLong(d.get("activityId")));
        ev.setItineraryId(asLong(d.get("itineraryId")));
        ev.setCategory(asString(d.get("category")));
        ev.setDetails(asDetails(d.get("details")));
        return ev;
    }

    private static PaymentAuditEvent buildPaymentAudit(Map<String, Object> d) {
        PaymentAuditEvent ev = new PaymentAuditEvent();
        ev.setAction(asString(d.get("action")));
        ev.setTimestamp(asTimestamp(d.get("timestamp")));
        ev.setBookingId(asLong(d.get("bookingId")));
        ev.setSettlementId(asLong(d.get("settlementId")));
        ev.setItineraryId(asLong(d.get("itineraryId")));
        ev.setMethod(asString(d.get("method")));
        ev.setAmount(asDouble(d.get("amount")));
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

    private static Double asDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); }
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
