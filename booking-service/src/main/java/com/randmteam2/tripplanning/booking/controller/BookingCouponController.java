package com.randmteam2.tripplanning.booking.controller;

import com.randmteam2.tripplanning.booking.model.BookingCoupon;
import com.randmteam2.tripplanning.booking.service.BookingCouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking-coupons")
public class BookingCouponController {

    @Autowired
    private BookingCouponService bookingCouponService;

    @GetMapping
    public ResponseEntity<List<BookingCoupon>> getAllBookingCoupons() {
        return ResponseEntity.ok(bookingCouponService.getAllBookingCoupons());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingCoupon> getBookingCouponById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingCouponService.getBookingCouponById(id));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<BookingCoupon>> getByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingCouponService.getByBookingId(bookingId));
    }

    @PostMapping
    public ResponseEntity<BookingCoupon> createBookingCoupon(@RequestBody BookingCoupon bookingCoupon) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingCouponService.createBookingCoupon(bookingCoupon));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingCoupon> updateBookingCoupon(@PathVariable Long id,
                                                             @RequestBody BookingCoupon bookingCoupon) {
        return ResponseEntity.ok(bookingCouponService.updateBookingCoupon(id, bookingCoupon));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookingCoupon(@PathVariable Long id) {
        bookingCouponService.deleteBookingCoupon(id);
        return ResponseEntity.noContent().build();
    }
}
