package com.randmteam2.tripplanning.user.observer;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class UserEventPublisher {

    private final List<EntityObserver> observers;

    public UserEventPublisher(List<EntityObserver> observers) {
        this.observers = observers != null ? new ArrayList<>(observers) : new ArrayList<>();
    }

    public void register(EntityObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    public List<EntityObserver> getObservers() {
        return Collections.unmodifiableList(observers);
    }

    public void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            try {
                observer.onEvent(eventType, payload);
            } catch (Exception e) {
                System.err.println("[WARN] observer "
                        + observer.getClass().getSimpleName()
                        + " failed for eventType " + eventType
                        + ": " + e.getMessage());
            }
        }
    }
}
