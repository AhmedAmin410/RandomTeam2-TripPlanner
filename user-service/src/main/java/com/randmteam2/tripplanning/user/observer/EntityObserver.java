package com.randmteam2.tripplanning.user.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}
