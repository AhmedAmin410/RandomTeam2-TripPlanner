package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.UserBookingSummaryDTO;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.model.BookingStatus;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

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
}