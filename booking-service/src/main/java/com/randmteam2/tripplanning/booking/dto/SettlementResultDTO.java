package com.randmteam2.tripplanning.booking.dto;

import java.math.BigDecimal;

public record SettlementResultDTO(
        Long settlementId,
        Long itineraryId,
        String status,
        BigDecimal amount,
        String failureReason
) {
}
