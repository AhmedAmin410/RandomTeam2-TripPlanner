package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.RefundCancellationRequest;
import com.randmteam2.tripplanning.booking.feign.BookingFeignClients;
import com.randmteam2.tripplanning.booking.feign.ItineraryServiceClient;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.model.BookingStatus;
import com.randmteam2.tripplanning.booking.model.BookingType;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEventRepository;
import com.randmteam2.tripplanning.booking.observer.BookingEvent;
import com.randmteam2.tripplanning.booking.observer.BookingEventPublisher;
import com.randmteam2.tripplanning.booking.repository.BookingCouponRepository;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import com.randmteam2.tripplanning.booking.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceRefundCancellationTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private BookingCouponRepository bookingCouponRepository;

    @Mock
    private PaymentAuditEventRepository auditRepository;

    @Mock
    private BookingEventPublisher eventPublisher;

    @Mock
    private BookingCacheInvalidationService cacheInvalidationService;

    @Mock
    private BookingFeignClients.UserServiceSafeClient userServiceClient;

    @Mock
    private BookingFeignClients.ItineraryServiceSafeClient itineraryServiceClient;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository,
                couponRepository,
                bookingCouponRepository,
                auditRepository,
                eventPublisher,
                cacheInvalidationService,
                userServiceClient,
                itineraryServiceClient
        );
    }

    @Test
    void processRefundCancellationAppliesPartialRefundAndInvalidatesCaches() {

        Booking booking = confirmedBooking();

        RefundCancellationRequest request = new RefundCancellationRequest();
        request.setReason("User changed travel dates");

        when(bookingRepository.findById(10L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.transitionBookingStatus(
                10L,
                BookingStatus.CONFIRMED,
                BookingStatus.CANCELLED
        )).thenReturn(1);

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubItinerary("PLANNED", LocalDate.now().plusDays(10));

        Booking saved = bookingService.processRefundCancellation(10L, request);

        assertThat(saved.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(saved.getBookingDetails())
                .containsEntry("refundAmount", 500.0)
                .containsEntry("tier", "MID")
                .containsEntry("strategyName", "MidCancellationRefundStrategy")
                .containsEntry("refundReason", "User changed travel dates")
                .containsKey("daysBeforeDeparture")
                .containsKey("refundedAt");

        ArgumentCaptor<BookingEvent> eventCaptor =
                ArgumentCaptor.forClass(BookingEvent.class);

        verify(eventPublisher).publish(eventCaptor.capture());

        BookingEvent event = eventCaptor.getValue();
        assertThat(event.getAction()).isEqualTo("REFUNDED");
        assertThat(event.getBookingId()).isEqualTo(10L);
        assertThat(event.getDetails())
                .containsEntry("refundAmount", 500.0)
                .containsEntry("originalAmount", 1000.0)
                .containsEntry("reason", "User changed travel dates");

        verify(cacheInvalidationService).evictRefundRelatedCaches();
    }

    @Test
    void processRefundCancellationDeniesStartedItineraryAndInvalidatesCaches() {

        Booking booking = confirmedBooking();

        RefundCancellationRequest request = new RefundCancellationRequest();
        request.setReason("Too late");

        when(bookingRepository.findById(10L))
                .thenReturn(Optional.of(booking));
        stubItinerary("IN_PROGRESS", LocalDate.now().plusDays(5));

        assertThatThrownBy(() ->
                bookingService.processRefundCancellation(10L, request)
        )
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(cacheInvalidationService).evictRefundRelatedCaches();
    }

    @Test
    void processRefundCancellationRejectsNonConfirmedBookingBeforeStrategySelection() {

        Booking booking = confirmedBooking();
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(10L))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.processRefundCancellation(
                        10L,
                        new RefundCancellationRequest()
                )
        )
                .isInstanceOf(ResponseStatusException.class);

        verify(itineraryServiceClient, never()).getItinerary(any());
    }

    private Booking confirmedBooking() {

        Booking booking = new Booking();

        booking.setId(10L);
        booking.setItineraryId(99L);
        booking.setUserId(68L);
        booking.setAmount(1000.0);
        booking.setType(BookingType.ACCOMMODATION);
        booking.setStatus(BookingStatus.CONFIRMED);

        booking.setBookingDetails(
                new HashMap<>(Map.of(
                        "providerName", "Hotel"
                ))
        );

        return booking;
    }

    private void stubItinerary(String status, LocalDate startDate) {
        doReturn(Optional.of(Map.<String, Object>of(
                "status", status,
                "startDate", startDate.toString()
        ))).when(itineraryServiceClient).getItinerary(99L);
    }
}
