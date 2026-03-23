package com.team2.tripplanning.booking.controller;

import com.team2.tripplanning.booking.dto.BookingRequestDTO;
import com.team2.tripplanning.booking.model.Booking;
import com.team2.tripplanning.booking.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

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