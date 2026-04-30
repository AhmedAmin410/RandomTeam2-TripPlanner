package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.PaymentHistoryEntryDTO;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEventRepository;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class PaymentHistoryService {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    // ANALYTICS_VIEWED is excluded; everything else payment-related is included.
    private static final Set<String> PAYMENT_ACTIONS = Set.of(
            "CREATED",
            "COMPLETED",
            "FAILED",
            "COUPON_APPLIED",
            "RETRY_ATTEMPTED",
            "REFUNDED",
            "REFUND_DENIED"
    );

    private final BookingRepository bookingRepository;
    private final PaymentAuditEventRepository auditRepository;

    public PaymentHistoryService(BookingRepository bookingRepository,
                                 PaymentAuditEventRepository auditRepository) {
        this.bookingRepository = bookingRepository;
        this.auditRepository = auditRepository;
    }

    @Cacheable(value = "booking-service",
            key = "'S5-F11::' + #bookingId + '::' + #page + '::' + #size")
    public Page<PaymentHistoryEntryDTO> getPaymentHistory(Long bookingId,
                                                         Integer page,
                                                         Integer size) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }

        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<com.randmteam2.tripplanning.booking.mongo.PaymentAuditEvent> events =
                auditRepository.findByBookingIdAndActionInOrderByTimestampAsc(
                        bookingId, PAYMENT_ACTIONS, pageable);

        List<PaymentHistoryEntryDTO> dtos = events.getContent().stream()
                .map(PaymentHistoryEntryDTO::new)
                .toList();

        return new PageImpl<>(dtos, pageable, events.getTotalElements());
    }
}
