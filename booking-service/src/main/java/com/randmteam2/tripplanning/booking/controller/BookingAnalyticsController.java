package com.randmteam2.tripplanning.booking.controller;

import com.randmteam2.tripplanning.booking.dto.DestinationSeasonRevenueDTO;
import com.randmteam2.tripplanning.booking.service.BookingAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingAnalyticsController {

    @Autowired private BookingAnalyticsService analyticsService;

    @GetMapping("/analytics/destination-season")
    public ResponseEntity<List<DestinationSeasonRevenueDTO>> getRevenueByDestinationAndSeason(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // log OUTSIDE the cached method so it fires even on cache hits
        analyticsService.logAnalyticsViewed();

        LocalDate from = startDate != null ? startDate : LocalDate.of(2000, 1, 1);
        LocalDate to = endDate != null ? endDate : LocalDate.of(2100, 1, 1);

        return ResponseEntity.ok(
                analyticsService.getRevenueByDestinationAndSeason(from, to));
    }
}
