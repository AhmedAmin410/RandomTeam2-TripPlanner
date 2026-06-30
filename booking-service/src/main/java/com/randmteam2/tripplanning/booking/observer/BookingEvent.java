package com.randmteam2.tripplanning.booking.observer;

import com.randmteam2.tripplanning.booking.model.Booking;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class BookingEvent {

    private final String action;
    private final Long bookingId;
    private final Long itineraryId;
    private final String method;
    private final Double amount;
    private final LocalDateTime timestamp;
    private final Map<String, Object> details;

    public BookingEvent(String action, Booking booking, Map<String, Object> details) {
        this.action = action;
        this.bookingId = booking.getId();
        this.itineraryId = booking.getItineraryId();
        this.method = booking.getType() != null ? booking.getType().name() : null;
        this.amount = booking.getAmount();
        this.timestamp = LocalDateTime.now();
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }

    public String getAction()                 { return action; }
    public Long getBookingId()                { return bookingId; }
    public Long getItineraryId()              { return itineraryId; }
    public String getMethod()                 { return method; }
    public Double getAmount()                 { return amount; }
    public LocalDateTime getTimestamp()       { return timestamp; }
    public Map<String, Object> getDetails()   { return details; }
}
