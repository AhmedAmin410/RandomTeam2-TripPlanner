package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.AppliedCouponDTO;
import com.randmteam2.tripplanning.booking.dto.BookingDetailsDTO;
import com.randmteam2.tripplanning.booking.model.*;
import com.randmteam2.tripplanning.booking.repository.BookingCouponRepository;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

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
}