package com.randmteam2.tripplanning.booking.dto;

public record CouponUsageDTO(
        Long couponId,
        String code,
        String discountType,
        Double discountValue,
        Long timesUsed,
        Double totalDiscountGiven,
        Boolean active,
        Boolean expired
) {}