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
        this.settlementRepository     = settlementRepository;
        this.bookingRepository        = bookingRepository;
        this.auditRepository          = auditRepository;
        this.paymentEventPublisher    = paymentEventPublisher;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    // ── itinerary.completed consumer ──────────────────────────────────────

    /**
     * Idempotent via DB unique constraint on itinerary_id (§1.5).
     * If the INSERT conflicts, the event is a duplicate — silently no-op.
     */
    @Transactional
    public void handleItineraryCompleted(ItineraryCompletedEvent event) {
        log.info("Consuming itinerary.completed for itineraryId={}", event.itineraryId());

        BigDecimal amount = event.totalAmount() != null
                ? event.totalAmount()
                : BigDecimal.valueOf(
                bookingRepository.sumConfirmedAmountByItineraryId(event.itineraryId()));

        Settlement settlement = new Settlement();
        settlement.setItineraryId(event.itineraryId());
        settlement.setUserId(event.userId());
        settlement.setAmount(amount);
        settlement.setStatus(SettlementStatus.PENDING);

        try {
            Settlement saved = settlementRepository.saveAndFlush(settlement);
            // Publish payment.initiated carrying the real PG settlementId
            paymentEventPublisher.publish("payment.initiated",
                    new PaymentInitiatedEvent(saved.getId(), saved.getItineraryId(), saved.getAmount()));
            writeSettlementAudit(saved, "SETTLEMENT_PENDING", null);
            log.info("Created PENDING settlement {} for itineraryId={}", saved.getId(), event.itineraryId());
        } catch (DataIntegrityViolationException duplicate) {
            // Duplicate itinerary.completed event — unique index on itinerary_id fires
            log.info("Duplicate itinerary.completed ignored for itineraryId={}", event.itineraryId());
        }
    }

    // ── itinerary.cancelled consumer ──────────────────────────────────────

    /**
     * Atomically cancel PENDING + refund CONFIRMED bookings.
     * Idempotent: atomic UPDATE rowcount=0 means already cancelled — no further events.
     */
    @Transactional
    public void handleItineraryCancelled(ItineraryCancelledEvent event) {
        log.info("Consuming itinerary.cancelled for itineraryId={}", event.itineraryId());

        // Cancel PENDING bookings atomically
        bookingRepository.transitionBookingsForItinerary(
                event.itineraryId(), BookingStatus.PENDING, BookingStatus.CANCELLED);

        // Refund CONFIRMED bookings atomically, one by one
        List<Booking> confirmedBookings = bookingRepository
                .findByItineraryIdAndStatus(event.itineraryId(), BookingStatus.CONFIRMED);
        Settlement settlement = settlementRepository.findByItineraryId(event.itineraryId()).orElse(null);

        BigDecimal refundAmount = BigDecimal.ZERO;
        for (Booking booking : confirmedBookings) {
            int updated = bookingRepository.transitionBookingStatus(
                    booking.getId(), BookingStatus.CONFIRMED, BookingStatus.CANCELLED);
            if (updated == 1) {
                refundAmount = refundAmount.add(BigDecimal.valueOf(booking.getAmount()));
                writeBookingRefundAudit(booking, event.reason());
                // Publish one payment.refunded per booking that was actually transitioned
                paymentEventPublisher.publish("payment.refunded",
                        new PaymentRefundedEvent(settlement != null ? settlement.getId() : null, event.itineraryId(),
                                BigDecimal.valueOf(booking.getAmount())));
                log.info("Refunded booking {} for itineraryId={}", booking.getId(), event.itineraryId());
            }
        }

        // Transition settlement row to REFUNDED
        if (settlement != null && settlement.getStatus() != SettlementStatus.REFUNDED) {
            settlementRepository.finishSettlement(
                    settlement.getId(),
                    settlement.getStatus(),
                    SettlementStatus.REFUNDED,
                    LocalDateTime.now(),
                    event.reason());
        }

        cacheInvalidationService.evictRefundRelatedCaches();
        log.info("Processed itinerary.cancelled for itineraryId={}, refundAmount={}",
                event.itineraryId(), refundAmount);
    }

    // ── POST /api/bookings/settlement/process ─────────────────────────────

    /**
     * Customer-initiated settlement. Uses SELECT FOR UPDATE as idempotency anchor.
     * PENDING → PROCESSING → COMPLETED or FAILED.
     */
    @Transactional
    public SettlementResultDTO processSettlement(SettlementProcessRequest request,
                                                 Long authenticatedUserId) {
        // (1) Lock the settlement row — 404 if saga hasn't run yet
        Settlement settlement = settlementRepository.lockByItineraryId(request.itineraryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Saga has not yet completed for this itinerary; cannot settle"));

        // (2) Inspect status
        if (settlement.getStatus() == SettlementStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement already in flight");
        }
        if (settlement.getStatus() == SettlementStatus.COMPLETED) {
            // Idempotent re-call — return prior result without re-publishing
            return toResult(settlement, null);
        }
        if (settlement.getStatus() == SettlementStatus.FAILED
                || settlement.getStatus() == SettlementStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement is already terminal");
        }

        // (3) Authorization — JWT uid must match body.userId and settlement.userId
        if (!settlement.getUserId().equals(request.userId())
                || (authenticatedUserId != null && !authenticatedUserId.equals(request.userId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User is not allowed to settle this itinerary");
        }

        // Amount must match (replay-with-altered-amount protection)
        if (request.amount() == null
                || request.amount().compareTo(settlement.getAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Settlement amount does not match saga amount");
        }

        // (4) Atomic PENDING → PROCESSING
        int processing = settlementRepository.transitionStatus(
                settlement.getId(), SettlementStatus.PENDING, SettlementStatus.PROCESSING);
        if (processing == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement already in flight");
        }

        // (5) Run settlement
        if (Boolean.TRUE.equals(request.simulateFailure())) {
            String reason = "Settlement rejected by processor";
            settlementRepository.finishSettlement(
                    settlement.getId(), SettlementStatus.PROCESSING, SettlementStatus.FAILED,
                    LocalDateTime.now(), reason);
            Settlement failed = settlementRepository.findById(settlement.getId()).orElseThrow();
            writeSettlementAudit(failed, "SETTLEMENT_FAILED", reason);
            paymentEventPublisher.publish("payment.failed",
                    new PaymentFailedEvent(failed.getId(), failed.getItineraryId(), reason));
            log.info("Settlement {} FAILED for itineraryId={}", failed.getId(), failed.getItineraryId());
            return toResult(failed, reason);
        }

        // (6) Success: PROCESSING → COMPLETED
        settlementRepository.finishSettlement(
                settlement.getId(), SettlementStatus.PROCESSING, SettlementStatus.COMPLETED,
                LocalDateTime.now(), null);
        Settlement completed = settlementRepository.findById(settlement.getId()).orElseThrow();
        writeSettlementAudit(completed, "SETTLEMENT_COMPLETED", null);
        paymentEventPublisher.publish("payment.completed",
                new PaymentCompletedEvent(
                        completed.getId(), completed.getItineraryId(), completed.getAmount()));
        log.info("Settlement {} COMPLETED for itineraryId={}", completed.getId(), completed.getItineraryId());

        return toResult(completed, null);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private SettlementResultDTO toResult(Settlement settlement, String failureReason) {
        return new SettlementResultDTO(
                settlement.getId(),
                settlement.getItineraryId(),
                settlement.getStatus().name(),
                settlement.getAmount(),
                failureReason != null ? failureReason : settlement.getFailureReason());
    }

    private void writeSettlementAudit(Settlement settlement, String action, String reason) {
        try {
            PaymentAuditEvent audit = new PaymentAuditEvent();
            audit.setSettlementId(settlement.getId());
            audit.setItineraryId(settlement.getItineraryId());
            audit.setAction(action);
            audit.setTimestamp(LocalDateTime.now());
            audit.setMethod("SETTLEMENT");
            audit.setAmount(settlement.getAmount().doubleValue());
            Map<String, Object> details = new HashMap<>();
            details.put("status", settlement.getStatus().name());
            if (reason != null) details.put("reason", reason);
            audit.setDetails(details);
            auditRepository.save(audit);
        } catch (Exception e) {
            log.warn("MongoDB settlement audit write failed for action={}: {}", action, e.getMessage());
        }
    }

    private void writeBookingRefundAudit(Booking booking, String reason) {
        try {
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
        } catch (Exception e) {
            log.warn("MongoDB booking refund audit write failed: {}", e.getMessage());
        }
    }
}
