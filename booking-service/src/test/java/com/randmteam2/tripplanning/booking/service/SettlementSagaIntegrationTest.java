package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.SettlementProcessRequest;
import com.randmteam2.tripplanning.booking.dto.SettlementResultDTO;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.model.BookingStatus;
import com.randmteam2.tripplanning.booking.model.BookingType;
import com.randmteam2.tripplanning.booking.model.Settlement;
import com.randmteam2.tripplanning.booking.model.SettlementStatus;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEventRepository;
import com.randmteam2.tripplanning.booking.observer.BookingEventPublisher;
import com.randmteam2.tripplanning.booking.repository.BookingCouponRepository;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import com.randmteam2.tripplanning.booking.repository.CouponRepository;
import com.randmteam2.tripplanning.booking.repository.SettlementRepository;
import com.randmteam2.tripplanning.booking.messaging.PaymentEventPublisher;
import com.randmteam2.tripplanning.contracts.dto.ConfirmedSummaryDTO;
import com.randmteam2.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.randmteam2.tripplanning.contracts.events.ItineraryCompletedEvent;
import com.randmteam2.tripplanning.contracts.events.PaymentCompletedEvent;
import com.randmteam2.tripplanning.contracts.events.PaymentFailedEvent;
import com.randmteam2.tripplanning.contracts.events.PaymentInitiatedEvent;
import com.randmteam2.tripplanning.contracts.events.PaymentRefundedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementSagaIntegrationTest {

    @Mock private SettlementRepository settlementRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentAuditEventRepository auditRepository;
    @Mock private PaymentEventPublisher paymentEventPublisher;
    @Mock private BookingCacheInvalidationService cacheInvalidationService;
    @Mock private CouponRepository couponRepository;
    @Mock private BookingCouponRepository bookingCouponRepository;
    @Mock private BookingEventPublisher bookingEventPublisher;

    private SettlementService settlementService;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(
                settlementRepository,
                bookingRepository,
                auditRepository,
                paymentEventPublisher,
                cacheInvalidationService);
        bookingService = new BookingService(
                bookingRepository,
                couponRepository,
                bookingCouponRepository,
                auditRepository,
                bookingEventPublisher,
                cacheInvalidationService);
    }

    @Test
    void scenarioAHappyPathCreatesSettlementAndPublishesPaymentCompleted() {
        BigDecimal amount = new BigDecimal("1250.00");
        ItineraryCompletedEvent completedEvent = new ItineraryCompletedEvent(99L, 68L, 5L, amount);

        when(settlementRepository.findByItineraryId(99L)).thenReturn(Optional.empty());
        when(settlementRepository.saveAndFlush(any(Settlement.class))).thenAnswer(invocation -> {
            Settlement settlement = invocation.getArgument(0);
            settlement.setId(7L);
            return settlement;
        });

        settlementService.handleItineraryCompleted(completedEvent);

        ArgumentCaptor<PaymentInitiatedEvent> initiatedCaptor =
                ArgumentCaptor.forClass(PaymentInitiatedEvent.class);
        verify(paymentEventPublisher).publish(eq("payment.initiated"), initiatedCaptor.capture());
        assertThat(initiatedCaptor.getValue().settlementId()).isEqualTo(7L);
        assertThat(initiatedCaptor.getValue().itineraryId()).isEqualTo(99L);
        assertThat(initiatedCaptor.getValue().amount()).isEqualByComparingTo(amount);

        Settlement pending = settlement(7L, 99L, 68L, amount, SettlementStatus.PENDING);
        Settlement paid = settlement(7L, 99L, 68L, amount, SettlementStatus.COMPLETED);
        when(settlementRepository.lockByItineraryId(99L)).thenReturn(Optional.of(pending));
        when(settlementRepository.transitionStatus(7L, SettlementStatus.PENDING, SettlementStatus.PROCESSING))
                .thenReturn(1);
        when(settlementRepository.findById(7L)).thenReturn(Optional.of(paid));

        SettlementResultDTO result = settlementService.processSettlement(
                new SettlementProcessRequest(99L, 68L, amount, false),
                68L);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.amount()).isEqualByComparingTo(amount);
        verify(settlementRepository).finishSettlement(
                eq(7L),
                eq(SettlementStatus.PROCESSING),
                eq(SettlementStatus.COMPLETED),
                any(),
                eq(null));

        ArgumentCaptor<PaymentCompletedEvent> paidCaptor =
                ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentEventPublisher).publish(eq("payment.completed"), paidCaptor.capture());
        assertThat(paidCaptor.getValue().settlementId()).isEqualTo(7L);
        assertThat(paidCaptor.getValue().itineraryId()).isEqualTo(99L);
        assertThat(paidCaptor.getValue().amount()).isEqualByComparingTo(amount);
    }

    @Test
    void scenarioBPaymentFailureCancelsConfirmedBookingsAndPublishesRefund() {
        BigDecimal amount = new BigDecimal("700.00");
        Settlement pending = settlement(8L, 100L, 68L, amount, SettlementStatus.PENDING);
        Settlement failed = settlement(8L, 100L, 68L, amount, SettlementStatus.FAILED);
        failed.setFailureReason("Settlement rejected by processor");

        when(settlementRepository.lockByItineraryId(100L)).thenReturn(Optional.of(pending));
        when(settlementRepository.transitionStatus(8L, SettlementStatus.PENDING, SettlementStatus.PROCESSING))
                .thenReturn(1);
        when(settlementRepository.findById(8L)).thenReturn(Optional.of(failed));

        SettlementResultDTO failedResult = settlementService.processSettlement(
                new SettlementProcessRequest(100L, 68L, amount, true),
                68L);

        assertThat(failedResult.status()).isEqualTo("FAILED");
        ArgumentCaptor<PaymentFailedEvent> failedCaptor =
                ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(paymentEventPublisher).publish(eq("payment.failed"), failedCaptor.capture());
        assertThat(failedCaptor.getValue().settlementId()).isEqualTo(8L);
        assertThat(failedCaptor.getValue().itineraryId()).isEqualTo(100L);

        Booking confirmed = booking(55L, 100L, 68L, 700.0, BookingStatus.CONFIRMED);
        when(bookingRepository.findByItineraryIdAndStatus(100L, BookingStatus.CONFIRMED))
                .thenReturn(List.of(confirmed));
        when(bookingRepository.transitionBookingStatus(55L, BookingStatus.CONFIRMED, BookingStatus.CANCELLED))
                .thenReturn(1);
        when(settlementRepository.findByItineraryId(100L)).thenReturn(Optional.of(failed));

        settlementService.handleItineraryCancelled(
                new ItineraryCancelledEvent(100L, 68L, 5L, "payment_failed"));

        verify(bookingRepository).transitionBookingsForItinerary(
                100L, BookingStatus.PENDING, BookingStatus.CANCELLED);
        verify(settlementRepository).finishSettlement(
                eq(8L),
                eq(SettlementStatus.FAILED),
                eq(SettlementStatus.REFUNDED),
                any(),
                eq("payment_failed"));
        verify(cacheInvalidationService).evictRefundRelatedCaches();

        ArgumentCaptor<PaymentRefundedEvent> refundedCaptor =
                ArgumentCaptor.forClass(PaymentRefundedEvent.class);
        verify(paymentEventPublisher).publish(eq("payment.refunded"), refundedCaptor.capture());
        assertThat(refundedCaptor.getValue().settlementId()).isEqualTo(8L);
        assertThat(refundedCaptor.getValue().itineraryId()).isEqualTo(100L);
        assertThat(refundedCaptor.getValue().refundAmount()).isEqualByComparingTo("700.00");
    }

    @Test
    void scenarioCNoConfirmedBookingsPreCheckLeavesSettlementUnavailable() {
        when(bookingRepository.countByItineraryIdAndStatus(200L, BookingStatus.CONFIRMED))
                .thenReturn(0L);
        when(bookingRepository.sumConfirmedAmountByItineraryId(200L)).thenReturn(0.0);

        ConfirmedSummaryDTO summary = bookingService.getConfirmedSummary(200L);

        assertThat(summary.count()).isZero();
        assertThat(summary.totalRevenue()).isZero();

        when(settlementRepository.lockByItineraryId(200L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.processSettlement(
                new SettlementProcessRequest(200L, 68L, BigDecimal.ZERO, false),
                68L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(paymentEventPublisher, never()).publish(anyString(), any());
    }

    private Settlement settlement(Long id, Long itineraryId, Long userId,
                                  BigDecimal amount, SettlementStatus status) {
        Settlement settlement = new Settlement();
        settlement.setId(id);
        settlement.setItineraryId(itineraryId);
        settlement.setUserId(userId);
        settlement.setAmount(amount);
        settlement.setStatus(status);
        return settlement;
    }

    private Booking booking(Long id, Long itineraryId, Long userId,
                            Double amount, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setItineraryId(itineraryId);
        booking.setUserId(userId);
        booking.setAmount(amount);
        booking.setType(BookingType.ACCOMMODATION);
        booking.setStatus(status);
        return booking;
    }
}
