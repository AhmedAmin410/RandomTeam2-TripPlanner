package com.randmteam2.tripplanning.booking.dto;

import java.util.Map;

public record UserBookingSummaryDTO(
        Long userId,
        Integer totalBookings,
        Double totalAmount,
        Map<String, Double> typeBreakdown
) {}