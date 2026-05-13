package com.randmteam2.tripplanning.contracts.dto;
public record DestinationBookingRevenueAggregateDTO(
    Long totalBookings,
    Double totalRevenue,
    Double averageBookingAmount
) {}
