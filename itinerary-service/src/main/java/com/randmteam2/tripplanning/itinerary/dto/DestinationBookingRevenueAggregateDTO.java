package com.randmteam2.tripplanning.itinerary.dto;

public record DestinationBookingRevenueAggregateDTO(
        long totalBookings,
        double totalRevenue,
        double averageBookingAmount
) {}