package com.randomteam2.tripplanning.destination.dto;

import java.math.BigDecimal;

public record DestinationBookingRevenueAggregateDTO(
        Long totalBookings,
        BigDecimal totalRevenue,
        BigDecimal averageBookingAmount
) {}
