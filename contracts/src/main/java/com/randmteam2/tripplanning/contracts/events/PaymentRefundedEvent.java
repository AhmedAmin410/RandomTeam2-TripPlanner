package com.randmteam2.tripplanning.contracts.events;
import java.math.BigDecimal;
public record PaymentRefundedEvent(Long settlementId, Long itineraryId, BigDecimal refundAmount) {}
