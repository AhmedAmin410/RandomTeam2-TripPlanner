package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.*;
import com.randmteam2.tripplanning.booking.feign.BookingFeignClients;
import com.randmteam2.tripplanning.booking.model.*;
import com.randmteam2.tripplanning.booking.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEvent;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEventRepository;
import com.randmteam2.tripplanning.booking.observer.BookingEvent;
import com.randmteam2.tripplanning.booking.observer.BookingEventPublisher;
import com.randmteam2.tripplanning.contracts.dto.ConfirmedSummaryDTO;
import com.randmteam2.tripplanning.contracts.dto.ItineraryAggregateDTO;
import com.randmteam2.tripplanning.contracts.dto.ItineraryBookingAggregateDTO;
import com.randmteam2.tripplanning.contracts.dto.UserBookingTotalDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import com.randmteam2.tripplanning.booking.strategy.*;


@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CouponRepository couponRepository;
    private final BookingCouponRepository bookingCouponRepository;
    private final PaymentAuditEventRepository auditRepository;
    private final BookingEventPublisher eventPublisher;
    private final BookingCacheInvalidationService cacheInvalidationService;
    private final BookingFeignClients.UserServiceSafeClient userServiceSafeClient;
    private final BookingFeignClients.ItineraryServiceSafeClient itineraryServiceSafeClient;
    private final BookingFeignClients.DestinationServiceSafeClient destinationServiceSafeClient;

    public BookingService(BookingRepository bookingRepository,
                          CouponRepository couponRepository,
                          BookingCouponRepository bookingCouponRepository,
                          PaymentAuditEventRepository auditRepository,
                          BookingEventPublisher eventPublisher,
                          BookingCacheInvalidationService cacheInvalidationService,
                          BookingFeignClients.UserServiceSafeClient userServiceSafeClient,
                          BookingFeignClients.ItineraryServiceSafeClient itineraryServiceSafeClient,
                          BookingFeignClients.DestinationServiceSafeClient destinationServiceSafeClient) {
        this.bookingRepository = bookingRepository;
        this.couponRepository = couponRepository;
        this.bookingCouponRepository = bookingCouponRepository;
        this.auditRepository = auditRepository;
        this.eventPublisher = eventPublisher;
        this.cacheInvalidationService = cacheInvalidationService;
        this.userServiceSafeClient = userServiceSafeClient;
        this.itineraryServiceSafeClient = itineraryServiceSafeClient;
        this.destinationServiceSafeClient = destinationServiceSafeClient;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> getRecommendations() {
        return bookingRepository.findAll();
    }

    @Cacheable(value = "booking-service", key = "'booking::' + #id")
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));
    }

    public Booking createBooking(Booking booking) {
        Booking saved = bookingRepository.save(booking);
        cacheInvalidationService.evictBookingReadCaches();
        return saved;
    }

    public Booking updateBooking(Long id, Booking booking) {
        Booking existing = getBookingById(id);
        booking.setId(id);
        booking.setCreatedAt(existing.getCreatedAt());
        Booking saved = bookingRepository.save(booking);
        cacheInvalidationService.evictBookingReadCaches();
        return saved;
    }

    public void deleteBooking(Long id) {
        getBookingById(id);
        bookingRepository.deleteById(id);
        cacheInvalidationService.evictBookingReadCaches();
    }

    // ── S5-F1 ─────────────────────────────────────────────────────────────
    @Cacheable(value = "booking-service",
            key = "'S5-F1::' + (#status == null ? 'ALL' : #status) + '::' + #startDate + '::' + #endDate")
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
        cacheInvalidationService.evictBookingReadCaches();
        return saved;
    }

    // ── S5-F3 S5-READ-DB: Compute aggregations in Java, no cross-service SQL ──
    @Cacheable(value = "booking-service", key = "'S5-F3::' + #userId")
    public UserBookingSummaryDTO getUserBookingSummary(Long userId) {
        // Verify user exists via safe feign wrapper
        if (userServiceSafeClient.getUser(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        // Fetch confirmed bookings for this user from local database only
        List<Booking> confirmedBookings = bookingRepository.findByUserIdAndStatus(userId, BookingStatus.CONFIRMED);

        // Compute aggregations entirely in Java
        Map<String, Double> typeBreakdown = new HashMap<>();
        int totalBookings = 0;
        double totalAmount = 0.0;

        for (Booking booking : confirmedBookings) {
            totalBookings++;
            totalAmount += booking.getAmount();

            String typeKey = booking.getType() != null ? booking.getType().name() : "UNKNOWN";
            double currentAmount = typeBreakdown.getOrDefault(typeKey, 0.0);
            typeBreakdown.put(typeKey, currentAmount + booking.getAmount());
        }

        return UserBookingSummaryDTO.builder()
                .userId(userId)
                .totalBookings(totalBookings)
                .totalAmount(totalAmount)
                .typeBreakdown(typeBreakdown)
                .build();
    }

    @Cacheable(value = "booking-service", key = "'S5-SYNC-USER-TOTAL::' + #userId + '::' + #startDate + '::' + #endDate")
    public UserBookingTotalDTO getUserBookingTotal(Long userId, String startDate, String endDate) {
        LocalDateTime start = parseStart(startDate);
        LocalDateTime end = parseEnd(endDate);
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
        }
        // Verify user exists via safe feign wrapper
        if (userServiceSafeClient.getUser(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
         Double totalAmountRaw = bookingRepository.sumConfirmedAmountByUserAndDateRange(userId, start, end);
         Long tripCountRaw = bookingRepository.countConfirmedTripsByUserAndDateRange(userId, start, end);

         Double totalAmount = totalAmountRaw != null ? totalAmountRaw : 0.0;
         Long tripCount = tripCountRaw != null ? tripCountRaw : 0L;

         return new UserBookingTotalDTO(userId, totalAmount, tripCount);
    }

     @Cacheable(value = "booking-service", key = "'S5-SYNC-AGGREGATE::' + #request")
     public ItineraryBookingAggregateDTO aggregateByItineraries(Map<String, Object> request) {
         List<Long> itineraryIds = extractItineraryIds(request.get("itineraryIds"));
         if (itineraryIds.isEmpty()) {
             return new ItineraryBookingAggregateDTO(itineraryIds, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
         }

         BookingStatus status = parseBookingStatus(String.valueOf(request.getOrDefault("status", "CONFIRMED")));
         LocalDateTime start = parseStart(Objects.toString(request.get("startDate"), null));
         LocalDateTime end = parseEnd(Objects.toString(request.get("endDate"), null));
         if (start.isAfter(end)) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
         }

         Long totalBookingsRaw = bookingRepository.countByItineraryIdsAndStatus(itineraryIds, status, start, end);
         Double totalRevenueRaw = bookingRepository.sumAmountByItineraryIdsAndStatus(itineraryIds, status, start, end);

         Long totalBookings = totalBookingsRaw != null ? totalBookingsRaw : 0L;
         BigDecimal totalRevenue = BigDecimal.valueOf(totalRevenueRaw != null ? totalRevenueRaw : 0.0);
         BigDecimal averageBookingAmount = totalBookings > 0 ? 
             totalRevenue.divide(BigDecimal.valueOf(totalBookings), BigDecimal.ROUND_HALF_UP) : 
             BigDecimal.ZERO;

         return new ItineraryBookingAggregateDTO(itineraryIds, totalBookings, totalRevenue, averageBookingAmount);
    }

    @Cacheable(value = "booking-service", key = "'S5-SYNC-CONFIRMED-SUMMARY::' + #itineraryId")
    public ConfirmedSummaryDTO getConfirmedSummary(Long itineraryId) {
        long count = bookingRepository.countByItineraryIdAndStatus(itineraryId, BookingStatus.CONFIRMED);
        Double totalRevenue = bookingRepository.sumConfirmedAmountByItineraryId(itineraryId);
        return new ConfirmedSummaryDTO(count, totalRevenue != null ? totalRevenue : 0.0);
    }

    // ── S5-F4 S5-READ-DB: Use safe feign wrapper, isolated booking creation ──
    @Transactional
    public Booking createBookingForItinerary(Long itineraryId,
                                             Map<String, Object> body,
                                             boolean simulateFailure) {
        // Fetch itinerary safely with fallback
        Object itinerary = itineraryServiceSafeClient.getItinerary(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found"));

        // Type-safe extraction of itinerary status
        @SuppressWarnings("unchecked")
        Map<String, Object> itineraryMap = (Map<String, Object>) itinerary;
        String itineraryStatus = (String) itineraryMap.get("status");
        if (!"PLANNED".equals(itineraryStatus) && !"IN_PROGRESS".equals(itineraryStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Itinerary must be PLANNED or IN_PROGRESS");
        }

        Booking booking = new Booking();
        booking.setItineraryId(itineraryId);
        booking.setUserId(((Number) body.getOrDefault("userId", 1)).longValue());
        booking.setAmount(((Number) body.get("amount")).doubleValue());
        booking.setType(BookingType.valueOf((String) body.get("type")));

        double surcharge = 0.0;
        Object destinationIdRaw = itineraryMap.get("destinationId");
        if (destinationIdRaw != null) {
            Long destinationId = ((Number) destinationIdRaw).longValue();
            int activeCount = itineraryServiceSafeClient.getDestinationActiveCount(destinationId);
            double multiplier = activeCount <= 5 ? 1.0 : activeCount <= 15 ? 1.3 : 1.6;
            surcharge = multiplier == 1.0 ? 0.0 : booking.getAmount() * (multiplier - 1) / multiplier;
        }

        Map<String, Object> details = new HashMap<>();
        if (body.containsKey("providerName")) details.put("providerName", body.get("providerName"));
        details.put("seasonalSurcharge", surcharge);
        booking.setBookingDetails(details);

        if (simulateFailure) {
            booking.setStatus(BookingStatus.FAILED);
            Booking saved = bookingRepository.save(booking);
            writeAuditEvent(saved, "FAILED");
            cacheInvalidationService.evictBookingReadCaches();
            return saved;
        }

        booking.setStatus(BookingStatus.PENDING);
        Booking saved = bookingRepository.save(booking);
        writeAuditEvent(saved, "CREATED");

        saved.setStatus(BookingStatus.CONFIRMED);
        saved = bookingRepository.save(saved);
        writeAuditEvent(saved, "COMPLETED");
        cacheInvalidationService.evictBookingReadCaches();

        return saved;
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
        cacheInvalidationService.evictBookingReadCaches();

        return saved;
    }

    // ── S5-F6 ─────────────────────────────────────────────────────────────
    @Cacheable(value = "booking-service", key = "'S5-F6::' + #startDate + '::' + #endDate")
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
        return RevenueReportDTO.builder()
                .totalRevenue(totalRevenue)
                .totalBookings(totalBookings)
                .averageBookingAmount(avg)
                .cancelledAmount(cancelledAmount)
                .cancelledCount(cancelledCount)
                .build();
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

        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("status", saved.getStatus().name());
        eventDetails.put("retryAttempt", attempt);
        eventDetails.put("confirmationNumber", details.get("confirmationNumber"));
        eventPublisher.publish(new BookingEvent("RETRY_ATTEMPTED", saved, eventDetails));
        cacheInvalidationService.evictBookingReadCaches();

        return saved;
    }

    // ── S5-F8 ─────────────────────────────────────────────────────────────
    @Cacheable(value = "booking-service", key = "'S5-F8::' + #bookingId")
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
        return BookingDetailsDTO.builder()
                .bookingId(booking.getId())
                .itineraryId(booking.getItineraryId())
                .userId(booking.getUserId())
                .originalAmount(booking.getAmount())
                .type(booking.getType().name())
                .status(booking.getStatus().name())
                .bookingDetails(booking.getBookingDetails())
                .appliedCoupons(appliedCoupons)
                .totalDiscount(totalDiscount)
                .finalAmount(finalAmount)
                .build();
    }

    // ── S5-F9 ─────────────────────────────────────────────────────────────
    @Cacheable(value = "booking-service", key = "'S5-F9::' + #limit")
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
            dtos.add(CouponUsageDTO.builder()
                    .couponId(couponId)
                    .code(code)
                    .discountType(discountType)
                    .discountValue(discountValue)
                    .timesUsed(timesUsed)
                    .totalDiscountGiven(totalDiscountGiven)
                    .active(active)
                    .expired(expired)
                    .build());
        }
        return dtos;
    }

    // ── S5-F12 S5-READ-DB: Atomic conditional state updates for idempotency ──
    @Transactional
    public Booking processRefundCancellation(Long bookingId, RefundCancellationRequest request) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only CONFIRMED bookings can be refunded");
        }

        // Fetch itinerary safely
        Object itinerary = itineraryServiceSafeClient.getItinerary(booking.getItineraryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked itinerary not found"));

        @SuppressWarnings("unchecked")
        Map<String, Object> itineraryMap = (Map<String, Object>) itinerary;

        Object startDateRaw = itineraryMap.get("startDate");
        LocalDate startDate;
        if (startDateRaw instanceof String s) {
            startDate = LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
        } else if (startDateRaw instanceof List<?> list && !list.isEmpty()) {
            // Jackson may deserialize date arrays as [year, month, day]
            int year  = ((Number) list.get(0)).intValue();
            int month = ((Number) list.get(1)).intValue();
            int day   = ((Number) list.get(2)).intValue();
            startDate = LocalDate.of(year, month, day);
        } else {
            startDate = LocalDate.now().plusDays(30);
        }

        String itiStatus = (String) itineraryMap.get("status");
        boolean itineraryStarted = "IN_PROGRESS".equals(itiStatus) || "COMPLETED".equals(itiStatus);

        RefundStrategySelector selector = new RefundStrategySelector();
        RefundStrategy strategy = selector.select(startDate, itineraryStarted);
        RefundResult result = strategy.calculateRefund(booking);

        if (strategy instanceof NoRefundStrategy) {
            writeAuditEventWithDetails(booking, "REFUND_DENIED", Map.of(
                    "strategyName", "NoRefundStrategy",
                    "reason", result.getReasonCode(),
                    "itineraryStatus", itiStatus
            ));
            cacheInvalidationService.evictRefundRelatedCaches();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "trip already started or completed");
        }

        // S5-READ-DB: Atomic UPDATE guard with conditional state check
        // This enforces idempotency: only 1 row can transition from CONFIRMED to CANCELLED
        int updated = bookingRepository.transitionBookingStatus(
                bookingId, BookingStatus.CONFIRMED, BookingStatus.CANCELLED);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Refund already in progress");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Map<String, Object> details = booking.getBookingDetails();
        if (details == null) details = new HashMap<>();
        details.put("refundAmount",        result.getRefundAmount());
        details.put("tier",                result.getTier());
        details.put("strategyName",        strategy.getClass().getSimpleName());
        details.put("refundReason",        request.getReason());
        details.put("daysBeforeDeparture", ChronoUnit.DAYS.between(LocalDate.now(), startDate));
        details.put("refundedAt",          LocalDateTime.now().toString());
        booking.setBookingDetails(details);
        Booking saved = bookingRepository.save(booking);

        writeAuditEventWithDetails(saved, "REFUNDED", Map.of(
                "strategyName",   strategy.getClass().getSimpleName(),
                "tier",           result.getTier(),
                "refundAmount",   result.getRefundAmount(),
                "originalAmount", booking.getAmount(),
                "reason",         request.getReason() != null ? request.getReason() : ""
        ));
        cacheInvalidationService.evictRefundRelatedCaches();

        return saved;
    }

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

    private LocalDateTime parseStart(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.of(2000, 1, 1, 0, 0);
        }
        return parseDateTime(value, true);
    }

    private LocalDateTime parseEnd(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.of(2100, 1, 1, 23, 59, 59, 999_000_000);
        }
        return parseDateTime(value, false);
    }

    private LocalDateTime parseDateTime(String value, boolean startOfDay) {
        String normalized = value.trim().replace(" ", "T");
        if (normalized.length() == 10) {
            LocalDate date = LocalDate.parse(normalized);
            return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59, 999_000_000);
        }
        return LocalDateTime.parse(normalized);
    }

    private BookingStatus parseBookingStatus(String status) {
        try {
            return BookingStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported booking status: " + status);
        }
    }

    private List<Long> extractItineraryIds(Object rawIds) {
        if (!(rawIds instanceof Collection<?> collection)) {
            return List.of();
        }
        return collection.stream()
                .map(this::toLong)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return null;
    }
}