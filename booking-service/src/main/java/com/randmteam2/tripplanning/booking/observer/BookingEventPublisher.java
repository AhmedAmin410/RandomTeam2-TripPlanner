package com.randmteam2.tripplanning.booking.observer;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class BookingEventPublisher {

    private final List<BookingEventObserver> observers;

    public BookingEventPublisher(List<BookingEventObserver> observers) {
        // Spring injects every bean implementing BookingEventObserver here.
        this.observers = observers != null ? new ArrayList<>(observers) : new ArrayList<>();
    }

    public void register(BookingEventObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregister(BookingEventObserver observer) {
        observers.remove(observer);
    }

    public List<BookingEventObserver> getObservers() {
        return Collections.unmodifiableList(observers);
    }

    public void publish(BookingEvent event) {
        if (event == null) return;
        for (BookingEventObserver o : observers) {
            try {
                o.onBookingEvent(event);
            } catch (Exception e) {
                // observers must never break the booking flow
                System.err.println("[WARN] observer "
                        + o.getClass().getSimpleName()
                        + " failed for action " + event.getAction()
                        + ": " + e.getMessage());
            }
        }
    }
}
