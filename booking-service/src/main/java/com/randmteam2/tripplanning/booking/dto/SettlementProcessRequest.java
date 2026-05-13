package com.randmteam2.tripplanning.booking.dto;

import java.math.BigDecimal;

public record SettlementProcessRequest(
        Long itineraryId,
        Long userId,
        BigDecimal amount,
        Boolean simulateFailure
) {
}
