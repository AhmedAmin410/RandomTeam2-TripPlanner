package com.randmteam2.tripplanning.itinerary.mongo;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}