package com.randmteam2.tripplanning.booking.mongo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentAuditEventRepository extends MongoRepository<PaymentAuditEvent, String> {

    Page<PaymentAuditEvent> findByBookingIdAndActionNotOrderByTimestampAsc(
            Long bookingId, String excludedAction, Pageable pageable);
}