package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.BookingRequestDTO;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.model.BookingStatus;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

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
}
