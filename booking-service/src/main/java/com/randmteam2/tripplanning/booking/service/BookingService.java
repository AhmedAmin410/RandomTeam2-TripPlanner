package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.*;
import com.randmteam2.tripplanning.booking.model.*;
import com.randmteam2.tripplanning.booking.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
// Add this import at the top of the file
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEvent;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEventRepository;
import com.randmteam2.tripplanning.booking.observer.BookingEvent;
import com.randmteam2.tripplanning.booking.observer.BookingEventPublisher;

import java.time.LocalDateTime;
import java.util.*;
import com.randmteam2.tripplanning.booking.strategy.*;
import com.randmteam2.tripplanning.booking.dto.RefundCancellationRequest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CouponRepository couponRepository;
    private final BookingCouponRepository bookingCouponRepository;
    private final PaymentAuditEventRepository auditRepository;
    private final BookingEventPublisher eventPublisher;

    public BookingService(BookingRepository bookingRepository,
                          CouponRepository couponRepository,
                          BookingCouponRepository bookingCouponRepository,
                          PaymentAuditEventRepository auditRepository,
                          BookingEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.couponRepository = couponRepository;
        this.bookingCouponRepository = bookingCouponRepository;
        this.auditRepository = auditRepository;
        this.eventPublisher = eventPublisher;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));
    }

    public Booking createBooking(Booking booking) {
        return bookingRepository.save(booking);
    }

    public Booking updateBooking(Long id, Booking booking) {
        Booking existing = getBookingById(id);
        booking.setId(id);
        booking.setCreatedAt(existing.getCreatedAt());
        return bookingRepository.save(booking);
    }

    public void deleteBooking(Long id) {
        getBookingById(id);
        bookingRepository.deleteById(id);
    }

    // ── S5-F1 ─────────────────────────────────────────────────────────────
    public List<Booking> searchBookings(String status, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        if (endDate == null) endDate = LocalDateTime.of(2100, 1, 1, 0, 0);
        return bookingRepository.searchBookings(status, startDate, endDate);
    }

    // ── S5-F2 ─────────────────────────────────────────────────────────────
    @Transactional
    public Booking cancelBooking(Long id, String reason) {
        Booking booking = getBookingById(id);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only CONFIRMED bookings can be cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        Map<String, Object> details = booking.getBookingDetails();
        if (details == null) details = new HashMap<>();
        details.put("cancellationReason", reason);
        details.put("cancelledAt", LocalDateTime.now().toString());
        booking.setBookingDetails(details);
        Booking saved = bookingRepository.save(booking);
        writeAuditEvent(saved, "REFUNDED");
        return saved;
    }

    // ── S5-F3 ─────────────────────────────────────────────────────────────
    // ── S5-F3 ─────────────────────────────────────────────────────────────
    public UserBookingSummaryDTO getUserBookingSummary(Long userId) {
        List<Object[]> userCheck = bookingRepository.checkUserExists(userId);
        if (userCheck == null || userCheck.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        List<Object[]> results = bookingRepository.getUserBookingSummary(userId);
        Map<String, Double> typeBreakdown = new HashMap<>();
        int totalBookings = 0;
        double totalAmount = 0;
        for (Object[] row : results) {
            String type = (String) row[0];
            int count = ((Number) row[1]).intValue();
            double amount = ((Number) row[2]).doubleValue();
            typeBreakdown.put(type, amount);
            totalBookings += count;
            totalAmount += amount;
        }
        return new UserBookingSummaryDTO(userId, totalBookings, totalAmount, typeBreakdown);
    }

    // ── S5-F4 ─────────────────────────────────────────────────────────────

    // ── S5-F4 ─────────────────────────────────────────────────────────────
    @Transactional
    public Booking createBookingForItinerary(Long itineraryId,
                                             Map<String, Object> body,
                                             boolean simulateFailure) {
        // validation (unchanged from M1)
        List<Object[]> itinerary = bookingRepository.findItineraryById(itineraryId);
        if (itinerary == null || itinerary.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }
        String itineraryStatus = (String) itinerary.get(0)[0];
        if (!itineraryStatus.equals("PLANNED") && !itineraryStatus.equals("IN_PROGRESS")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Itinerary must be PLANNED or IN_PROGRESS");
        }

        // build booking (unchanged from M1)
        Booking booking = new Booking();
        booking.setItineraryId(itineraryId);
        booking.setUserId(((Number) body.getOrDefault("userId", 1)).longValue());
        booking.setAmount(((Number) body.get("amount")).doubleValue());
        booking.setType(BookingType.valueOf((String) body.get("type")));

        // NEW: compute seasonalSurcharge
        Long destinationId = bookingRepository.findDestinationIdByItineraryId(itineraryId);
        double surcharge = 0.0;
        if (destinationId != null) {
            long activeCount = bookingRepository.countActiveItinerariesForDestination(destinationId);
            double multiplier = activeCount <= 5 ? 1.0 : activeCount <= 15 ? 1.3 : 1.6;
            surcharge = (multiplier == 1.0) ? 0.0
                    : booking.getAmount() * (multiplier - 1) / multiplier;
        }

        Map<String, Object> details = new HashMap<>();
        if (body.containsKey("providerName")) details.put("providerName", body.get("providerName"));
        details.put("seasonalSurcharge", surcharge);
        booking.setBookingDetails(details);

        // NEW: simulateFailure support
        if (simulateFailure) {
            booking.setStatus(BookingStatus.FAILED);
            Booking saved = bookingRepository.save(booking);
            writeAuditEvent(saved, "FAILED");
            return saved;
        }

        // normal flow: PENDING then CONFIRMED
        booking.setStatus(BookingStatus.PENDING);
        Booking saved = bookingRepository.save(booking);
        writeAuditEvent(saved, "CREATED");

        saved.setStatus(BookingStatus.CONFIRMED);
        saved = bookingRepository.save(saved);
        writeAuditEvent(saved, "COMPLETED");

        return saved;
    }

    // helper: write a PaymentAuditEvent to MongoDB
    private void writeAuditEvent(Booking booking, String action) {
        try {
            PaymentAuditEvent ev = new PaymentAuditEvent();
            ev.setBookingId(booking.getId());
            ev.setAction(action);
            ev.setTimestamp(LocalDateTime.now());
            ev.setMethod(booking.getType().name());
            ev.setAmount(booking.getAmount());
            ev.setDetails(Map.of("status", booking.getStatus().name()));
            auditRepository.save(ev);
        } catch (Exception e) {
            System.err.println("[WARN] MongoDB audit write failed: " + e.getMessage());
        }
    }
    // ── S5-F5 ─────────────────────────────────────────────────────────────
    @Transactional
    public Booking applyCoupon(Long bookingId, Long couponId) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cannot apply coupon to a confirmed/cancelled booking");
        }
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Coupon not found"));
        if (!coupon.getActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon is inactive");
        }
        if (coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon is expired");
        }
        if (coupon.getCurrentUses() >= coupon.getMaxUses()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon limit reached");
        }
        boolean alreadyApplied = bookingCouponRepository
                .existsByBookingIdAndCouponId(bookingId, couponId);
        if (alreadyApplied) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupon already applied");
        }
        double discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = booking.getAmount() * coupon.getDiscountValue() / 100;
        } else {
            discount = coupon.getDiscountValue();
        }
        if (discount > booking.getAmount()) discount = booking.getAmount();

        BookingCoupon bc = new BookingCoupon();
        bc.setBooking(booking);
        bc.setCoupon(coupon);
        bc.setDiscountApplied(discount);
        bc.setAppliedAt(LocalDateTime.now());
        bookingCouponRepository.save(bc);

        coupon.setCurrentUses(coupon.getCurrentUses() + 1);
        couponRepository.save(coupon);
        Booking saved = bookingRepository.save(booking);

        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("status", saved.getStatus().name());
        eventDetails.put("couponId", coupon.getId());
        eventDetails.put("couponCode", coupon.getCode());
        eventDetails.put("discountType", coupon.getDiscountType().name());
        eventDetails.put("discountApplied", discount);
        eventPublisher.publish(new BookingEvent("COUPON_APPLIED", saved, eventDetails));

        return saved;
    }

    // ── S5-F6 ─────────────────────────────────────────────────────────────
    public RevenueReportDTO getRevenueReport(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        if (endDate == null) endDate = LocalDateTime.of(2100, 1, 1, 0, 0);
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must be before endDate");
        }
        List<Object[]> results = bookingRepository.getRevenueReport(startDate, endDate);
        double totalRevenue = 0;
        int totalBookings = 0;
        double cancelledAmount = 0;
        int cancelledCount = 0;
        for (Object[] row : results) {
            String status = (String) row[0];
            int count = ((Number) row[1]).intValue();
            double amount = ((Number) row[2]).doubleValue();
            if ("CONFIRMED".equals(status)) {
                totalRevenue = amount;
                totalBookings = count;
            } else if ("CANCELLED".equals(status)) {
                cancelledAmount = amount;
                cancelledCount = count;
            }
        }
        double avg = totalBookings > 0 ? totalRevenue / totalBookings : 0;
        return new RevenueReportDTO(totalRevenue, totalBookings, avg, cancelledAmount, cancelledCount);
    }

    // ── S5-F7 ─────────────────────────────────────────────────────────────
    @Transactional
    public Booking retryBooking(Long id) {
        Booking booking = getBookingById(id);
        if (booking.getStatus() != BookingStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only FAILED bookings can be retried");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        Map<String, Object> details = booking.getBookingDetails();
        if (details == null) details = new HashMap<>();
        int attempt = ((Number) details.getOrDefault("retryAttempt", 0)).intValue() + 1;
        details.put("retryAttempt", attempt);
        details.put("confirmationNumber", "RETRY-" + id + "-" + attempt);
        booking.setBookingDetails(details);
        Booking saved = bookingRepository.save(booking);
        writeAuditEvent(saved, "RETRY_ATTEMPTED");
        return saved;
    }

    // ── S5-F8 ─────────────────────────────────────────────────────────────
    public BookingDetailsDTO getBookingDetails(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        List<BookingCoupon> coupons = bookingCouponRepository.findByBookingId(bookingId);
        List<AppliedCouponDTO> appliedCoupons = coupons.stream()
                .map(bc -> new AppliedCouponDTO(
                        bc.getCoupon().getCode(),
                        bc.getCoupon().getDiscountType().name(),
                        bc.getDiscountApplied(),
                        bc.getAppliedAt()))
                .toList();
        double totalDiscount = appliedCoupons.stream()
                .mapToDouble(c -> c.discountApplied())
                .sum();
        double finalAmount = booking.getAmount() - totalDiscount;
        return new BookingDetailsDTO(
                booking.getId(),
                booking.getItineraryId(),
                booking.getUserId(),
                booking.getAmount(),
                booking.getType().name(),
                booking.getStatus().name(),
                booking.getBookingDetails(),
                appliedCoupons,
                totalDiscount,
                finalAmount
        );
    }

    // ── S5-F9 ─────────────────────────────────────────────────────────────
    public List<CouponUsageDTO> getTopUsedCoupons(int limit) {
        List<Object[]> results = bookingRepository.getTopUsedCoupons(limit);
        List<CouponUsageDTO> dtos = new ArrayList<>();
        for (Object[] row : results) {
            Long couponId = ((Number) row[0]).longValue();
            String code = (String) row[1];
            String discountType = (String) row[2];
            double discountValue = ((Number) row[3]).doubleValue();
            long timesUsed = ((Number) row[4]).longValue();
            double totalDiscountGiven = ((Number) row[5]).doubleValue();
            boolean active = (Boolean) row[6];
            LocalDateTime expiryDate = ((java.sql.Timestamp) row[7]).toLocalDateTime();
            boolean expired = expiryDate.isBefore(LocalDateTime.now());
            dtos.add(new CouponUsageDTO(couponId, code, discountType, discountValue,
                    timesUsed, totalDiscountGiven, active, expired));
        }
        return dtos;
    }

    // ── S5-F12 ────────────────────────────────────────────────────────────
    @Transactional
    public Booking processRefundCancellation(Long bookingId, RefundCancellationRequest request) {
        // 1. fetch booking
        Booking booking = getBookingById(bookingId);

        // 2. must be CONFIRMED
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only CONFIRMED bookings can be refunded");
        }

        // 3. fetch itinerary startDate and status
        List<Object[]> rows = bookingRepository.findItineraryStartDateAndStatus(booking.getItineraryId());
        if (rows == null || rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked itinerary not found");
        }
        java.sql.Date startDateSql = (java.sql.Date) rows.get(0)[0];
        String itiStatus = (String) rows.get(0)[1];
        LocalDate startDate = startDateSql.toLocalDate();
        boolean itineraryStarted = "IN_PROGRESS".equals(itiStatus) || "COMPLETED".equals(itiStatus);

        // 4. select strategy — no if/else chains here, selector does it
        RefundStrategySelector selector = new RefundStrategySelector();
        RefundStrategy strategy = selector.select(startDate, itineraryStarted);
        RefundResult result = strategy.calculateRefund(booking);

        // 5. NoRefund → log REFUND_DENIED then throw 400
        if (strategy instanceof NoRefundStrategy) {
            writeAuditEventWithDetails(booking, "REFUND_DENIED", Map.of(
                    "strategyName", "NoRefundStrategy",
                    "reason", result.getReasonCode(),
                    "itineraryStatus", itiStatus
            ));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "trip already started or completed");
        }

        // 6. update booking
        booking.setStatus(BookingStatus.CANCELLED);
        Map<String, Object> details = booking.getBookingDetails();
        if (details == null) details = new HashMap<>();
        details.put("refundAmount",       result.getRefundAmount());
        details.put("tier",               result.getTier());
        details.put("strategyName",       strategy.getClass().getSimpleName());
        details.put("refundReason",       request.getReason());
        details.put("daysBeforeDeparture", ChronoUnit.DAYS.between(LocalDate.now(), startDate));
        details.put("refundedAt",         LocalDateTime.now().toString());
        booking.setBookingDetails(details);
        Booking saved = bookingRepository.save(booking);

        // 7. log REFUNDED
        writeAuditEventWithDetails(saved, "REFUNDED", Map.of(
                "strategyName",  strategy.getClass().getSimpleName(),
                "tier",          result.getTier(),
                "refundAmount",  result.getRefundAmount(),
                "originalAmount", booking.getAmount(),
                "reason",        request.getReason() != null ? request.getReason() : ""
        ));

        return saved;
    }

    // enhanced audit helper with custom details
    private void writeAuditEventWithDetails(Booking booking, String action, Map<String, Object> details) {
        try {
            PaymentAuditEvent ev = new PaymentAuditEvent();
            ev.setBookingId(booking.getId());
            ev.setAction(action);
            ev.setTimestamp(LocalDateTime.now());
            ev.setMethod(booking.getType().name());
            ev.setAmount(booking.getAmount());
            ev.setDetails(details);
            auditRepository.save(ev);
        } catch (Exception e) {
            System.err.println("[WARN] MongoDB write failed: " + e.getMessage());
        }
    }
}