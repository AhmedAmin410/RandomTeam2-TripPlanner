package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.CouponUsageDTO;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.repository.BookingCouponRepository;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
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
}