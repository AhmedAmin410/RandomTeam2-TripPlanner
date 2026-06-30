package com.randmteam2.tripplanning.booking.observer;

import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEvent;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEventRepository;
import org.springframework.stereotype.Component;

@Component
public class MongoAuditTrailObserver implements BookingEventObserver {

    private final PaymentAuditEventRepository auditRepository;

    public MongoAuditTrailObserver(PaymentAuditEventRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public void onBookingEvent(BookingEvent event) {
        try {
            PaymentAuditEvent ev = new PaymentAuditEvent();
            ev.setBookingId(event.getBookingId());
            ev.setItineraryId(event.getItineraryId());
            ev.setAction(event.getAction());
            ev.setTimestamp(event.getTimestamp());
            ev.setMethod(event.getMethod());
            ev.setAmount(event.getAmount());
            ev.setDetails(event.getDetails());
            auditRepository.save(ev);
        } catch (Exception e) {
            System.err.println("[WARN] MongoDB audit write failed: " + e.getMessage());
        }
    }
}
