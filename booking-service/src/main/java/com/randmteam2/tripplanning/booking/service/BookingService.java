package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.List;

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
        Booking existing = getBookingById(id); // get existing from DB
        booking.setId(id);
        booking.setCreatedAt(existing.getCreatedAt()); // keep original createdAt
        return bookingRepository.save(booking);
    }

    public void deleteBooking(Long id) {
        getBookingById(id); // throws 404 if not found
        bookingRepository.deleteById(id);
    }
    public List<Booking> searchBookings(String status, LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRepository.searchBookings(status, startDate, endDate);
    }
}
