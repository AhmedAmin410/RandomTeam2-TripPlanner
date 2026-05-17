package com.randomteam2.tripplanning.destination.dto;

import java.math.BigDecimal;

/**
 * Aggregate returned by itinerary-service at
 * GET /api/itineraries/destination/{destinationId}/booking-revenue?startDate=&endDate=
 *
 * itinerary-service builds this by:
 *   1. Querying its own DB for itinerary IDs where destination_id = ?
 *   2. Feigning to booking-service POST /api/bookings/aggregate-by-itineraries
 *      with {itineraryIds, startDate, endDate, status:"CONFIRMED"}
 *
 * destination-service never touches itinerary-postgres or booking-postgres directly (M3).
 */
public record DestinationBookingRevenueAggregateDTO(
        Long totalBookings,
        BigDecimal totalRevenue,
        BigDecimal averageBookingAmount
) {}