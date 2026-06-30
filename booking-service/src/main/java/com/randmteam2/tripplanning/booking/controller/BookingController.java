package com.randmteam2.tripplanning.booking.controller;

import com.randmteam2.tripplanning.booking.dto.BookingDetailsDTO;
import com.randmteam2.tripplanning.booking.dto.PaymentHistoryEntryDTO;
import com.randmteam2.tripplanning.booking.dto.RevenueReportDTO;
import com.randmteam2.tripplanning.booking.dto.SettlementProcessRequest;
import com.randmteam2.tripplanning.booking.dto.SettlementResultDTO;
import com.randmteam2.tripplanning.booking.dto.UserBookingSummaryDTO;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.service.BookingService;
import com.randmteam2.tripplanning.booking.service.PaymentHistoryService;
import com.randmteam2.tripplanning.booking.service.SettlementService;
import com.randmteam2.tripplanning.contracts.dto.ConfirmedSummaryDTO;
import com.randmteam2.tripplanning.contracts.dto.ItineraryBookingAggregateDTO;
import com.randmteam2.tripplanning.contracts.dto.UserBookingTotalDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.randmteam2.tripplanning.booking.dto.RefundCancellationRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentHistoryService paymentHistoryService;
    private final SettlementService settlementService;

    public BookingController(BookingService bookingService,
                             PaymentHistoryService paymentHistoryService,
                             SettlementService settlementService) {
        this.bookingService = bookingService;
        this.paymentHistoryService = paymentHistoryService;
        this.settlementService = settlementService;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<Booking>> getRecommendations() {
        return ResponseEntity.ok(bookingService.getRecommendations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody Booking booking) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(booking));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> updateBooking(@PathVariable Long id,
                                                 @RequestBody Booking booking) {
        return ResponseEntity.ok(bookingService.updateBooking(id, booking));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }


    // ── S5-F1 ─────────────────────────────────────────────────────────────
    @GetMapping("/search")
    public ResponseEntity<List<Booking>> searchBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDateTime start = parseDateTimeParam(startDate, LocalDateTime.of(2000, 1, 1, 0, 0));
        LocalDateTime end = parseDateTimeParam(endDate, LocalDateTime.of(2100, 1, 1, 0, 0));
        return ResponseEntity.ok(bookingService.searchBookings(status, start, end));
    }

    // ── S5-F2 ─────────────────────────────────────────────────────────────
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancelBooking(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, body.get("reason")));
    }

    // ── S5-F3 ─────────────────────────────────────────────────────────────
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<UserBookingSummaryDTO> getUserBookingSummary(
            @PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getUserBookingSummary(userId));
    }

    @GetMapping("/user/{userId}/total")
    public ResponseEntity<UserBookingTotalDTO> getUserBookingTotal(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(bookingService.getUserBookingTotal(userId, startDate, endDate));
    }

    @PostMapping("/aggregate-by-itineraries")
    public ResponseEntity<ItineraryBookingAggregateDTO> aggregateByItineraries(
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(bookingService.aggregateByItineraries(request));
    }

    @GetMapping("/itinerary/{itineraryId}/confirmed-summary")
    public ResponseEntity<ConfirmedSummaryDTO> getConfirmedSummary(@PathVariable Long itineraryId) {
        return ResponseEntity.ok(bookingService.getConfirmedSummary(itineraryId));
    }

    // ── S5 Saga: confirmed-count alias (itinerary-service saga pre-check) ──
    @GetMapping("/itinerary/{itineraryId}/confirmed-count")
    public ResponseEntity<Long> getConfirmedCount(@PathVariable Long itineraryId) {
        ConfirmedSummaryDTO summary = bookingService.getConfirmedSummary(itineraryId);
        return ResponseEntity.ok(summary.count());
    }

    // ── S5-F4 ─────────────────────────────────────────────────────────────
    // ── S5-F4 ─────────────────────────────────────────────────────────────
    @PostMapping("/itinerary/{itineraryId}")
    public ResponseEntity<Booking> createBookingForItinerary(
            @PathVariable Long itineraryId,
            @RequestBody java.util.Map<String, Object> body,
            @RequestParam(defaultValue = "false") boolean simulateFailure) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBookingForItinerary(itineraryId, body, simulateFailure));
    }

    // ── S5-F5 ─────────────────────────────────────────────────────────────
    @PostMapping("/{bookingId}/coupons/{couponId}")
    public ResponseEntity<Booking> applyCoupon(
            @PathVariable Long bookingId,
            @PathVariable Long couponId) {
        return ResponseEntity.ok(bookingService.applyCoupon(bookingId, couponId));
    }

    // ── S5-F6 ─────────────────────────────────────────────────────────────
    @GetMapping("/reports/revenue")
    public ResponseEntity<RevenueReportDTO> getRevenueReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDateTime start = parseDateTimeParam(startDate, LocalDateTime.of(2000, 1, 1, 0, 0));
        LocalDateTime end = parseDateTimeParam(endDate, LocalDateTime.of(2100, 1, 1, 0, 0));
        return ResponseEntity.ok(bookingService.getRevenueReport(start, end));
    }
    // ── S5-F7 ─────────────────────────────────────────────────────────────
    @PutMapping("/{id}/retry")
    public ResponseEntity<Booking> retryBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.retryBooking(id));
    }

    // ── S5-F8 ─────────────────────────────────────────────────────────────
    @GetMapping("/{bookingId}/details")
    public ResponseEntity<BookingDetailsDTO> getBookingDetails(
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBookingDetails(bookingId));
    }

    // ── S5-F9 ─────────────────────────────────────────────────────────────
    @GetMapping("/coupons/top-used")
    public ResponseEntity<List<?>> getTopUsedCoupons(
            @RequestParam int limit) {
        return ResponseEntity.ok(bookingService.getTopUsedCoupons(limit));
    }

    // ── S5-F12 ────────────────────────────────────────────────────────────
    @PostMapping("/{id}/refund-cancellation-tier")
    public ResponseEntity<Booking> processRefundCancellation(
            @PathVariable Long id,
            @RequestBody RefundCancellationRequest request) {
        return ResponseEntity.ok(bookingService.processRefundCancellation(id, request));
    }

    // ── S5-F11 ────────────────────────────────────────────────────────────
    @GetMapping("/{id}/payment-history")
    public ResponseEntity<Map<String, Object>> getPaymentHistory(
            @PathVariable Long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Long callerId = auth != null && auth.getDetails() instanceof Number n ? n.longValue() : null;
        boolean admin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        List<PaymentHistoryEntryDTO> content =
                paymentHistoryService.getPaymentHistory(id, page, size, callerId, admin);
        return ResponseEntity.ok(Map.of(
                "content", content,
                "page", page == null || page < 0 ? 0 : page,
                "size", size == null || size <= 0 ? PaymentHistoryService.DEFAULT_PAGE_SIZE : size
        ));
    }

    @PostMapping("/settlement/process")
    public ResponseEntity<SettlementResultDTO> processSettlement(
            @RequestBody SettlementProcessRequest request,
            @RequestHeader(name = "X-User-Id", required = false) Long authenticatedUserId) {
        return ResponseEntity.ok(settlementService.processSettlement(request, authenticatedUserId));
    }

    private LocalDateTime parseDateTimeParam(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().replace(" ", "T");
        if (normalized.length() == 10) {
            return LocalDate.parse(normalized).atStartOfDay();
        }
        return LocalDateTime.parse(normalized);
    }
}
