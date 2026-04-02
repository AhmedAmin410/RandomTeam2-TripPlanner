package com.randmteam2.tripplanning.booking.controller;

import com.randmteam2.tripplanning.booking.dto.BookingDetailsDTO;
import com.randmteam2.tripplanning.booking.dto.CouponUsageDTO;
import com.randmteam2.tripplanning.booking.dto.UserBookingSummaryDTO;
import com.randmteam2.tripplanning.booking.dto.BookingRequestDTO;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.model.BookingCoupon;
import com.randmteam2.tripplanning.booking.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
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

    @GetMapping("/search")
    public ResponseEntity<List<Booking>> searchBookings(
            @RequestParam(required = false) String status,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        return ResponseEntity.ok(bookingService.searchBookings(status, startDate, endDate));
    }

    // S5-F3: User Booking Summary
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<UserBookingSummaryDTO> getUserBookingSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getUserBookingSummary(userId));
    }

    // S5-F7: Retry Failed Booking
    @PutMapping("/{id}/retry")
    public ResponseEntity<Booking> retryBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.retryFailedBooking(id));
    }

    // S5-F5: Apply Coupon to Booking
    @PostMapping("/{bookingId}/coupons/{couponId}")
    public ResponseEntity<BookingCoupon> applyCouponToBooking(@PathVariable Long bookingId,
                                                              @PathVariable Long couponId) {
        return ResponseEntity.ok(bookingService.applyCouponToBooking(bookingId, couponId));
    }

    // S5-F8: Booking Details with Coupons
    @GetMapping("/{bookingId}/details")
    public ResponseEntity<BookingDetailsDTO> getBookingDetails(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBookingDetails(bookingId));
    }

    // S5-F9: Most Used Coupons Report
    @GetMapping("/coupons/top-used")
    public ResponseEntity<List<CouponUsageDTO>> getTopUsedCoupons(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(bookingService.getTopUsedCoupons(limit));
    }

    // S5-F6: Revenue Report
    @GetMapping("/revenue")
    public ResponseEntity<Double> getRevenue(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(bookingService.getRevenue(start, end));
    }

    // S5-F4: Create Booking for Itinerary
    @PostMapping("/itinerary/{itineraryId}")
    public ResponseEntity<Booking> createBookingForItinerary(
            @PathVariable Long itineraryId,
            @RequestBody BookingRequestDTO bookingRequest) {
        
        try {
            Booking booking = bookingService.createBooking(itineraryId, bookingRequest);
            return new ResponseEntity<>(booking, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating booking", e);
        }

}

}
