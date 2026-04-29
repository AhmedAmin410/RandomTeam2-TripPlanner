package com.randmteam2.tripplanning.booking.dto;

import java.time.LocalDateTime;

public record AppliedCouponDTO(
        String couponCode,
        String discountType,
        Double discountApplied,
        LocalDateTime appliedAt
) {}