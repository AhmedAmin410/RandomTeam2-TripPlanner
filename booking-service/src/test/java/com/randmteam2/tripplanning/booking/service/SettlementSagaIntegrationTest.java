package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.SettlementProcessRequest;
import com.randmteam2.tripplanning.booking.dto.SettlementResultDTO;
import com.randmteam2.tripplanning.booking.feign.BookingFeignClients;
import com.randmteam2.tripplanning.booking.messaging.PaymentEventPublisher;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.model.BookingStatus;
import com.randmteam2.tripplanning.booking.model.BookingType;
import com.randmteam2.tripplanning.booking.model.Settlement;
import com.randmteam2.tripplanning.booking.model.SettlementStatus;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEventRepository;
import com.randmteam2.tripplanning.booking.observer.BookingEventPublisher;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import com.randmteam2.tripplanning.booking.repository.SettlementRepository;
import com.randmteam2.tripplanning.contracts.dto.ConfirmedSummaryDTO;
import com.randmteam2.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.randmteam2.tripplanning.contracts.events.ItineraryCompletedEvent;
import com.randmteam2.tripplanning.contracts.events.PaymentCompletedEvent;
import com.randmteam2.tripplanning.contracts.events.PaymentFailedEvent;
import com.randmteam2.tripplanning.contracts.events.PaymentInitiatedEvent;
import com.randmteam2.tripplanning.contracts.events.PaymentRefundedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({SettlementService.class, BookingService.class})
class SettlementSagaIntegrationTest {

    @Autowired private SettlementService settlementService;
    @Autowired private BookingService bookingService;
    @Autowired private SettlementRepository settlementRepository;
    @Autowired private BookingRepository bookingRepository;

    @MockBean private PaymentAuditEventRepository auditRepository;
    @MockBean private PaymentEventPublisher paymentEventPublisher;
    @MockBean private BookingCacheInvalidationService cacheInvalidationService;
    @MockBean private BookingEventPublisher bookingEventPublisher;
    @MockBean private BookingFeignClients.UserServiceSafeClient userServiceClient;
    @MockBean private BookingFeignClients.ItineraryServiceSafeClient itineraryServiceClient;

    @Test
    void scenarioAHappyPathCreatesSettlementAndPublishesPaymentCompleted() {
        BigDecimal amount = new BigDecimal("1250.00");

        settlementService.handleItineraryCompleted(
                new ItineraryCompletedEvent(99L, 68L, 5L, amount));

        Settlement pending = settlementRepository.findByItineraryId(99L).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(SettlementStatus.PENDING);
        assertThat(pending.getAmount()).isEqualByComparingTo(amount);

        ArgumentCaptor<PaymentInitiatedEvent> initiatedCaptor =
                ArgumentCaptor.forClass(PaymentInitiatedEvent.class);
        verify(paymentEventPublisher).publish(eq("payment.initiated"), initiatedCaptor.capture());
        assertThat(initiatedCaptor.getValue().settlementId()).isEqualTo(pending.getId());
        assertThat(initiatedCaptor.getValue().itineraryId()).isEqualTo(99L);
        assertThat(initiatedCaptor.getValue().amount()).isEqualByComparingTo(amount);

        SettlementResultDTO result = settlementService.processSettlement(
                new SettlementProcessRequest(99L, 68L, amount, false),
                68L);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.amount()).isEqualByComparingTo(amount);
        Settlement completed = settlementRepository.findByItineraryId(99L).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(completed.getSettledAt()).isNotNull();

        ArgumentCaptor<PaymentCompletedEvent> completedCaptor =
                ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentEventPublisher).publish(eq("payment.completed"), completedCaptor.capture());
        assertThat(completedCaptor.getValue().settlementId()).isEqualTo(pending.getId());
        assertThat(completedCaptor.getValue().itineraryId()).isEqualTo(99L);
        assertThat(completedCaptor.getValue().amount()).isEqualByComparingTo(amount);
    }

    @Test
    void scenarioBPaymentFailureCancelsConfirmedBookingsAndPublishesRefund() {
        Booking confirmed = booking(100L, 68L, 700.0, BookingStatus.CONFIRMED);
        bookingRepository.saveAndFlush(confirmed);

        BigDecimal amount = new BigDecimal("700.00");
        settlementService.handleItineraryCompleted(
                new ItineraryCompletedEvent(100L, 68L, 5L, amount));
        Long settlementId = settlementRepository.findByItineraryId(100L).orElseThrow().getId();

        SettlementResultDTO failedResult = settlementService.processSettlement(
                new SettlementProcessRequest(100L, 68L, amount, true),
                68L);

        assertThat(failedResult.status()).isEqualTo("FAILED");
        Settlement failed = settlementRepository.findByItineraryId(100L).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(SettlementStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("Settlement rejected by processor");

        ArgumentCaptor<PaymentFailedEvent> failedCaptor =
                ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(paymentEventPublisher).publish(eq("payment.failed"), failedCaptor.capture());
        assertThat(failedCaptor.getValue().settlementId()).isEqualTo(settlementId);
        assertThat(failedCaptor.getValue().itineraryId()).isEqualTo(100L);

        settlementService.handleItineraryCancelled(
                new ItineraryCancelledEvent(100L, 68L, 5L, "payment_failed"));

        Booking cancelled = bookingRepository.findById(confirmed.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        Settlement refunded = settlementRepository.findByItineraryId(100L).orElseThrow();
        assertThat(refunded.getStatus()).isEqualTo(SettlementStatus.REFUNDED);
        verify(cacheInvalidationService).evictRefundRelatedCaches();

        ArgumentCaptor<PaymentRefundedEvent> refundedCaptor =
                ArgumentCaptor.forClass(PaymentRefundedEvent.class);
        verify(paymentEventPublisher).publish(eq("payment.refunded"), refundedCaptor.capture());
        assertThat(refundedCaptor.getValue().settlementId()).isEqualTo(settlementId);
        assertThat(refundedCaptor.getValue().itineraryId()).isEqualTo(100L);
        assertThat(refundedCaptor.getValue().refundAmount()).isEqualByComparingTo("700.00");
    }

    @Test
    void scenarioCNoConfirmedBookingsPreCheckLeavesSettlementUnavailable() {
        ConfirmedSummaryDTO summary = bookingService.getConfirmedSummary(200L);

        assertThat(summary.count()).isZero();
        assertThat(summary.totalRevenue()).isZero();

        assertThatThrownBy(() -> settlementService.processSettlement(
                new SettlementProcessRequest(200L, 68L, BigDecimal.ZERO, false),
                68L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(settlementRepository.findByItineraryId(200L)).isEmpty();
        verify(paymentEventPublisher, never()).publish(anyString(), any());
    }

    private Booking booking(Long itineraryId, Long userId, Double amount, BookingStatus status) {
        Booking booking = new Booking();
        booking.setItineraryId(itineraryId);
        booking.setUserId(userId);
        booking.setAmount(amount);
        booking.setType(BookingType.ACCOMMODATION);
        booking.setStatus(status);
        return booking;
    }
}
