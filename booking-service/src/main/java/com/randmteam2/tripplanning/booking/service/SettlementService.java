package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.SettlementProcessRequest;
import com.randmteam2.tripplanning.booking.dto.SettlementResultDTO;
import com.randmteam2.tripplanning.booking.messaging.PaymentEventPublisher;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.model.BookingStatus;
import com.randmteam2.tripplanning.booking.model.Settlement;
import com.randmteam2.tripplanning.booking.model.SettlementStatus;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEvent;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEventRepository;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import com.randmteam2.tripplanning.booking.repository.SettlementRepository;
import com.randmteam2.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.randmteam2.tripplanning.contracts.events.ItineraryCompletedEvent;
import com.randmteam2.tripplanning.contracts.events.PaymentCompletedEvent;
import com.randmteam2.tripplanning.contracts.events.PaymentFailedEvent;
import com.randmteam2.tripplanning.contracts.events.PaymentInitiatedEvent;
import com.randmteam2.tripplanning.contracts.events.PaymentRefundedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final SettlementRepository settlementRepository;
    private final BookingRepository bookingRepository;
    private final PaymentAuditEventRepository auditRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final BookingCacheInvalidationService cacheInvalidationService;

    public SettlementService(SettlementRepository settlementRepository,
                             BookingRepository bookingRepository,
                             PaymentAuditEventRepository auditRepository,
                             PaymentEventPublisher paymentEventPublisher,
                             BookingCacheInvalidationService cacheInvalidationService) {
        this.settlementRepository = settlementRepository;
        this.bookingRepository = bookingRepository;
        this.auditRepository = auditRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Transactional
    public void handleItineraryCompleted(ItineraryCompletedEvent event) {
        if (settlementRepository.findByItineraryId(event.itineraryId()).isPresent()) {
            log.info("Settlement already exists for itinerary={}", event.itineraryId());
            return;
        }

        BigDecimal amount = event.totalAmount() != null
                ? event.totalAmount()
                : BigDecimal.valueOf(bookingRepository.sumConfirmedAmountByItineraryId(event.itineraryId()));

        Settlement settlement = new Settlement();
        settlement.setItineraryId(event.itineraryId());
        settlement.setUserId(event.userId());
        settlement.setAmount(amount);
        settlement.setStatus(SettlementStatus.PENDING);

        try {
            Settlement saved = settlementRepository.saveAndFlush(settlement);
            paymentEventPublisher.publish("payment.initiated",
                    new PaymentInitiatedEvent(saved.getId(), saved.getItineraryId(), saved.getAmount()));
            writeSettlementAudit(saved, "SETTLEMENT_PENDING", null);
            log.info("Created pending settlement {} for itinerary={}", saved.getId(), saved.getItineraryId());
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Duplicate itinerary.completed ignored for itinerary={}", event.itineraryId());
        }
    }

    @Transactional
    public void handleItineraryCancelled(ItineraryCancelledEvent event) {
        List<Booking> confirmedBookings = bookingRepository.findByItineraryIdAndStatus(
                event.itineraryId(), BookingStatus.CONFIRMED);

        bookingRepository.transitionBookingsForItinerary(
                event.itineraryId(), BookingStatus.PENDING, BookingStatus.CANCELLED);

        BigDecimal refundAmount = BigDecimal.ZERO;
        for (Booking booking : confirmedBookings) {
            int updated = bookingRepository.transitionBookingStatus(
                    booking.getId(), BookingStatus.CONFIRMED, BookingStatus.CANCELLED);
            if (updated == 1) {
                refundAmount = refundAmount.add(BigDecimal.valueOf(booking.getAmount()));
                writeBookingRefundAudit(booking, event.reason());
            }
        }

        Settlement settlement = settlementRepository.findByItineraryId(event.itineraryId()).orElse(null);
        if (settlement != null && settlement.getStatus() != SettlementStatus.REFUNDED) {
            settlementRepository.finishSettlement(
                    settlement.getId(),
                    settlement.getStatus(),
                    SettlementStatus.REFUNDED,
                    LocalDateTime.now(),
                    event.reason());
        }

        if (settlement != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            paymentEventPublisher.publish("payment.refunded",
                    new PaymentRefundedEvent(settlement.getId(), event.itineraryId(), refundAmount));
        }
        cacheInvalidationService.evictRefundRelatedCaches();
        log.info("Processed itinerary.cancelled for itinerary={}, refundAmount={}",
                event.itineraryId(), refundAmount);
    }

    @Transactional
    public SettlementResultDTO processSettlement(SettlementProcessRequest request, Long authenticatedUserId) {
        Settlement settlement = settlementRepository.lockByItineraryId(request.itineraryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Saga has not yet completed for this itinerary; cannot settle"));

        if (settlement.getStatus() == SettlementStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement already in flight");
        }
        if (settlement.getStatus() == SettlementStatus.COMPLETED) {
            return toResult(settlement, null);
        }
        if (settlement.getStatus() == SettlementStatus.FAILED || settlement.getStatus() == SettlementStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement is already terminal");
        }

        if (!settlement.getUserId().equals(request.userId())
                || (authenticatedUserId != null && !authenticatedUserId.equals(request.userId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not allowed to settle this itinerary");
        }
        if (request.amount() == null || request.amount().compareTo(settlement.getAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Settlement amount does not match saga amount");
        }

        int processing = settlementRepository.transitionStatus(
                settlement.getId(), SettlementStatus.PENDING, SettlementStatus.PROCESSING);
        if (processing == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement already in flight");
        }

        if (Boolean.TRUE.equals(request.simulateFailure())) {
            String reason = "Settlement rejected by processor";
            settlementRepository.finishSettlement(
                    settlement.getId(), SettlementStatus.PROCESSING, SettlementStatus.FAILED,
                    LocalDateTime.now(), reason);
            Settlement failed = settlementRepository.findById(settlement.getId()).orElseThrow();
            writeSettlementAudit(failed, "SETTLEMENT_FAILED", reason);
            paymentEventPublisher.publish("payment.failed",
                    new PaymentFailedEvent(failed.getId(), failed.getItineraryId(), reason));
            return toResult(failed, reason);
        }

        settlementRepository.finishSettlement(
                settlement.getId(), SettlementStatus.PROCESSING, SettlementStatus.COMPLETED,
                LocalDateTime.now(), null);
        Settlement completed = settlementRepository.findById(settlement.getId()).orElseThrow();
        writeSettlementAudit(completed, "SETTLEMENT_COMPLETED", null);
        paymentEventPublisher.publish("payment.completed",
                new PaymentCompletedEvent(completed.getId(), completed.getItineraryId(), completed.getAmount()));
        return toResult(completed, null);
    }

    private SettlementResultDTO toResult(Settlement settlement, String failureReason) {
        return new SettlementResultDTO(
                settlement.getId(),
                settlement.getItineraryId(),
                settlement.getStatus().name(),
                settlement.getAmount(),
                failureReason != null ? failureReason : settlement.getFailureReason());
    }

    private void writeSettlementAudit(Settlement settlement, String action, String reason) {
        PaymentAuditEvent audit = new PaymentAuditEvent();
        audit.setSettlementId(settlement.getId());
        audit.setItineraryId(settlement.getItineraryId());
        audit.setAction(action);
        audit.setTimestamp(LocalDateTime.now());
        audit.setMethod("SETTLEMENT");
        audit.setAmount(settlement.getAmount().doubleValue());
        Map<String, Object> details = new HashMap<>();
        details.put("status", settlement.getStatus().name());
        if (reason != null) {
            details.put("reason", reason);
        }
        audit.setDetails(details);
        auditRepository.save(audit);
    }

    private void writeBookingRefundAudit(Booking booking, String reason) {
        PaymentAuditEvent audit = new PaymentAuditEvent();
        audit.setBookingId(booking.getId());
        audit.setItineraryId(booking.getItineraryId());
        audit.setAction("REFUNDED");
        audit.setTimestamp(LocalDateTime.now());
        audit.setMethod(booking.getType() != null ? booking.getType().name() : null);
        audit.setAmount(booking.getAmount());
        audit.setDetails(Map.of(
                "status", BookingStatus.CANCELLED.name(),
                "reason", reason != null ? reason : "itinerary_cancelled"));
        auditRepository.save(audit);
    }
}
