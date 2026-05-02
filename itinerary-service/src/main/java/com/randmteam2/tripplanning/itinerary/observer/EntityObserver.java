package com.randmteam2.tripplanning.itinerary.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}