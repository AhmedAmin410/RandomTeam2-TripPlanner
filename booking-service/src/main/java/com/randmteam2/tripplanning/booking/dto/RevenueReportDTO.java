package com.randmteam2.tripplanning.booking.dto;

public record RevenueReportDTO(
        Double totalRevenue,
        Integer totalBookings,
        Double averageBookingAmount,
        Double cancelledAmount,
        Integer cancelledCount
) {}