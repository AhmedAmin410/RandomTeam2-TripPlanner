package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.AppliedCouponDTO;
import com.randmteam2.tripplanning.booking.dto.BookingDetailsDTO;
import com.randmteam2.tripplanning.booking.dto.CouponUsageDTO;
import com.randmteam2.tripplanning.booking.dto.UserBookingSummaryDTO;
import com.randmteam2.tripplanning.booking.dto.BookingRequestDTO;
import com.randmteam2.tripplanning.booking.dto.RevenueReportDTO;
import com.randmteam2.tripplanning.booking.model.*;
import com.randmteam2.tripplanning.booking.repository.BookingCouponRepository;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import com.randmteam2.tripplanning.booking.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private BookingCouponRepository bookingCouponRepository;

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found with id: " + id));
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

    // S5-F2: Cancel Booking with Refund
    @Transactional
    public Booking cancelBooking(Long id, String reason) {
        Booking booking = getBookingById(id);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is not CONFIRMED");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        Map<String, Object> details = booking.getBookingDetails();
        if (details == null) {
            details = new HashMap<>();
        }
        details.put("cancellationReason", reason);
        details.put("cancelledAt", LocalDateTime.now().toString());
        booking.setBookingDetails(details);

        return bookingRepository.save(booking);
    }

    public List<Booking> searchBookings(String status, LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRepository.searchBookings(status, startDate, endDate);
    }

    // S5-F3: User Booking Summary
    public UserBookingSummaryDTO getUserBookingSummary(Long userId) {
        int userCount = bookingRepository.countUserById(userId);
        if (userCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + userId);
        }

        List<Booking> confirmedBookings = bookingRepository.findByUserIdAndStatus(userId, BookingStatus.CONFIRMED);

        int totalBookings = confirmedBookings.size();
        double totalAmount = 0.0;
        Map<String, Double> typeBreakdown = new HashMap<>();

        for (Booking booking : confirmedBookings) {
            totalAmount += booking.getAmount();
            String typeName = booking.getType().name();
            typeBreakdown.merge(typeName, booking.getAmount(), Double::sum);
        }

        return new UserBookingSummaryDTO(userId, totalBookings, totalAmount, typeBreakdown);
    }

    // S5-F5: Apply Coupon to Booking
    @Transactional
    public BookingCoupon applyCouponToBooking(Long bookingId, Long couponId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found with id: " + bookingId));

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Coupon not found with id: " + couponId));

        // booking must be PENDING
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cannot apply coupon to a confirmed/cancelled booking");
        }

        // coupon must be active
        if (!coupon.getActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon is not active");
        }

        // coupon must not be expired
        if (coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon has expired");
        }

        // coupon usage limit check
        if (coupon.getCurrentUses() >= coupon.getMaxUses()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon usage limit reached");
        }

        // check if coupon already applied to this booking
        if (bookingCouponRepository.findByBookingAndCoupon(booking, coupon).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "coupon already applied");
        }

        // calculate discount
        double discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = booking.getAmount() * (coupon.getDiscountValue() / 100.0);
        } else {
            discount = coupon.getDiscountValue();
        }
        // cap discount at booking amount
        discount = Math.min(discount, booking.getAmount());

        // create BookingCoupon record
        BookingCoupon bookingCoupon = new BookingCoupon();
        bookingCoupon.setBooking(booking);
        bookingCoupon.setCoupon(coupon);
        bookingCoupon.setDiscountApplied(discount);
        bookingCoupon.setAppliedAt(LocalDateTime.now());

        BookingCoupon saved = bookingCouponRepository.save(bookingCoupon);

        // increment currentUses on coupon
        coupon.setCurrentUses(coupon.getCurrentUses() + 1);
        couponRepository.save(coupon);

        // save the booking as well
        bookingRepository.save(booking);

        return saved;
    }

    // S5-F8: Booking Details with Coupons
    public BookingDetailsDTO getBookingDetails(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found with id: " + bookingId));

        List<BookingCoupon> bookingCoupons = bookingCouponRepository.findByBookingId(bookingId);

        List<AppliedCouponDTO> appliedCoupons = new ArrayList<>();
        double totalDiscount = 0.0;

        for (BookingCoupon bc : bookingCoupons) {
            Coupon coupon = bc.getCoupon();
            AppliedCouponDTO dto = new AppliedCouponDTO(
                    coupon.getCode(),
                    coupon.getDiscountType().name(),
                    bc.getDiscountApplied(),
                    bc.getAppliedAt()
            );
            appliedCoupons.add(dto);
            totalDiscount += bc.getDiscountApplied();
        }

        BookingDetailsDTO details = new BookingDetailsDTO();
        details.setBookingId(booking.getId());
        details.setItineraryId(booking.getItineraryId());
        details.setUserId(booking.getUserId());
        details.setOriginalAmount(booking.getAmount());
        details.setType(booking.getType().name());
        details.setStatus(booking.getStatus().name());
        details.setBookingDetails(booking.getBookingDetails());
        details.setAppliedCoupons(appliedCoupons);
        details.setTotalDiscount(totalDiscount);
        details.setFinalAmount(booking.getAmount() - totalDiscount);

        return details;
    }

    // S5-F9: Most Used Coupons Report
    public List<CouponUsageDTO> getTopUsedCoupons(int limit) {
        List<Object[]> rows = bookingCouponRepository.findTopUsedCoupons(limit);
        List<CouponUsageDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            Long couponId = ((Number) row[0]).longValue();
            String code = (String) row[1];
            String discountType = (String) row[2];
            Double discountValue = ((Number) row[3]).doubleValue();
            Long timesUsed = ((Number) row[4]).longValue();
            Double totalDiscountGiven = ((Number) row[5]).doubleValue();
            Boolean active = (Boolean) row[6];
            LocalDateTime expiryDate = ((Timestamp) row[7]).toLocalDateTime();

            result.add(new CouponUsageDTO(couponId, code, discountType, discountValue,
                    timesUsed, totalDiscountGiven, active, expiryDate));
        }

        return result;
    }

    // S5-F4: Create Booking for Itinerary
    @Transactional
    public Booking createBooking(Long itineraryId, BookingRequestDTO request) {
        // Verify itinerary exists and get status
        String status = bookingRepository.findItineraryStatusById(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found"));

        // Validate status
        if (!"PLANNED".equals(status) && !"IN_PROGRESS".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Itinerary must be PLANNED or IN_PROGRESS");
        }

        // Get user ID from itinerary
        Long userId = bookingRepository.findUserIdByItineraryId(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User ID not found for itinerary"));

        // Create Booking
        Booking booking = new Booking();
        booking.setItineraryId(itineraryId);
        booking.setUserId(userId);
        booking.setAmount(request.getAmount());
        booking.setType(request.getType());
        booking.setStatus(BookingStatus.PENDING);

        Map<String, Object> details = new HashMap<>();
        if (request.getProviderName() != null) {
            details.put("providerName", request.getProviderName());
        }
        booking.setBookingDetails(details);

        return bookingRepository.save(booking);
    }

    public RevenueReportDTO getRevenueReport(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
        }
        Double rawRevenue = bookingRepository.calculateRevenue(BookingStatus.CONFIRMED, startDate, endDate);
        Long rawBookings = bookingRepository.countBookingsByStatusAndDateRange(BookingStatus.CONFIRMED, startDate, endDate);
        Double rawCancelledAmount = bookingRepository.calculateRevenue(BookingStatus.CANCELLED, startDate, endDate);
        Long rawCancelledCount = bookingRepository.countBookingsByStatusAndDateRange(BookingStatus.CANCELLED, startDate, endDate);

        double totalRevenue = rawRevenue != null ? rawRevenue : 0.0;
        long totalBookings = rawBookings != null ? rawBookings : 0L;
        double avgAmount = totalBookings > 0 ? totalRevenue / totalBookings : 0.0;
        double cancelledAmount = rawCancelledAmount != null ? rawCancelledAmount : 0.0;
        long cancelledCount = rawCancelledCount != null ? rawCancelledCount : 0L;

        return new RevenueReportDTO(totalRevenue, totalBookings, avgAmount, cancelledAmount, cancelledCount);
    }
}
