package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.model.BookingCoupon;
import com.randmteam2.tripplanning.booking.repository.BookingCouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BookingCouponService {

    @Autowired
    private BookingCouponRepository bookingCouponRepository;

    public List<BookingCoupon> getAllBookingCoupons() {
        return bookingCouponRepository.findAll();
    }

    public BookingCoupon getBookingCouponById(Long id) {
        return bookingCouponRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "BookingCoupon not found with id: " + id));
    }

    public List<BookingCoupon> getByBookingId(Long bookingId) {
        return bookingCouponRepository.findByBookingId(bookingId);
    }

    public BookingCoupon createBookingCoupon(BookingCoupon bookingCoupon) {
        return bookingCouponRepository.save(bookingCoupon);
    }

    public BookingCoupon updateBookingCoupon(Long id, BookingCoupon bookingCoupon) {
        BookingCoupon existing = getBookingCouponById(id);
        bookingCoupon.setId(id);
        return bookingCouponRepository.save(bookingCoupon);
    }

    public void deleteBookingCoupon(Long id) {
        getBookingCouponById(id);
        bookingCouponRepository.deleteById(id);
    }
}
