package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.model.*;
import com.randmteam2.tripplanning.booking.repository.BookingCouponRepository;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import com.randmteam2.tripplanning.booking.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

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

    public List<Booking> searchBookings(String status, LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRepository.searchBookings(status, startDate, endDate);
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
}