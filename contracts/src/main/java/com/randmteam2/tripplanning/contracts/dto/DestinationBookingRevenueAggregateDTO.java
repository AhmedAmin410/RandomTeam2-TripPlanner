package com.randmteam2.tripplanning.contracts.dto;

import java.io.Serializable;

public record DestinationBookingRevenueAggregateDTO(
    Long totalBookings,
    Double totalRevenue,
    Double averageBookingAmount
) implements Serializable {}
