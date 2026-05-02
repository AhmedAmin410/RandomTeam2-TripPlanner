package com.randmteam2.tripplanning.booking.dto;

import java.util.List;
import java.util.Map;

public record BookingDetailsDTO(
        Long bookingId,
        Long itineraryId,
        Long userId,
        Double originalAmount,
        String type,
        String status,
        Map<String, Object> bookingDetails,
        List<AppliedCouponDTO> appliedCoupons,
        Double totalDiscount,
        Double finalAmount
) {}