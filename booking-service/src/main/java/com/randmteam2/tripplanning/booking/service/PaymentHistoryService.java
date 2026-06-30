package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.PaymentHistoryEntryDTO;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEvent;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEventRepository;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
            "REFUNDED"
    );

    private final BookingRepository bookingRepository;
    private final PaymentAuditEventRepository auditRepository;

    public PaymentHistoryService(BookingRepository bookingRepository,
                                 PaymentAuditEventRepository auditRepository) {
        this.bookingRepository = bookingRepository;
        this.auditRepository = auditRepository;
    }

    public List<PaymentHistoryEntryDTO> getPaymentHistory(Long bookingId,
                                                         Integer page,
                                                         Integer size,
                                                         Long callerUserId,
                                                         boolean admin) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!admin && (callerUserId == null || !callerUserId.equals(booking.getUserId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<com.randmteam2.tripplanning.booking.mongo.PaymentAuditEvent> events =
                auditRepository.findByBookingIdAndItineraryIdAndActionInAndAmountOrderByTimestampAsc(
                        bookingId, booking.getItineraryId(), PAYMENT_ACTIONS, booking.getAmount(), pageable);

        List<PaymentHistoryEntryDTO> dtos = events.getContent().stream()
                .map(PaymentHistoryEntryDTO::new)
                .toList();

        PaymentAuditEvent viewed = new PaymentAuditEvent();
        viewed.setBookingId(bookingId);
        viewed.setItineraryId(booking.getItineraryId());
        viewed.setAction("ANALYTICS_VIEWED");
        viewed.setTimestamp(LocalDateTime.now());
        viewed.setDetails(Map.of("endpoint", "S5-F11"));
        auditRepository.save(viewed);

        return dtos;
    }
}
