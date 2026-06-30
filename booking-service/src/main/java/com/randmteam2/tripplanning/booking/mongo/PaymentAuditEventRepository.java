package com.randmteam2.tripplanning.booking.mongo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;

public interface PaymentAuditEventRepository extends MongoRepository<PaymentAuditEvent, String> {

    Page<PaymentAuditEvent> findByBookingIdAndActionNotOrderByTimestampAsc(
            Long bookingId, String excludedAction, Pageable pageable);

    // S5-F11: payment history — only payment-related actions, ASC by timestamp
    Page<PaymentAuditEvent> findByBookingIdAndActionInOrderByTimestampAsc(
            Long bookingId, Collection<String> actions, Pageable pageable);

    Page<PaymentAuditEvent> findByBookingIdAndItineraryIdAndActionInOrderByTimestampAsc(
            Long bookingId, Long itineraryId, Collection<String> actions, Pageable pageable);

    Page<PaymentAuditEvent> findByBookingIdAndItineraryIdAndActionInAndAmountOrderByTimestampAsc(
            Long bookingId, Long itineraryId, Collection<String> actions, Double amount, Pageable pageable);
}
