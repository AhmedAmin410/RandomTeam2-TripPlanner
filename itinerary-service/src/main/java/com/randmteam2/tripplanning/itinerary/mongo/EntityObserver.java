package com.randmteam2.tripplanning.itinerary.mongo;
/**
 * DP-2: Classical GoF Observer Pattern implementation.
 * MongoEventLogger is a concrete observer that listens for entity state changes
 * and persists them as ItineraryEvent documents in MongoDB.
 * Soft dependency Ã¢â‚¬â€ Mongo exceptions are caught and logged at WARN level,
 * never rethrown to the caller.
 */
public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}
