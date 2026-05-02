package com.randomteam2.tripplanning.destination.observer;

public interface EntityObserver {

    void onEvent(String eventType, Object payload);
}