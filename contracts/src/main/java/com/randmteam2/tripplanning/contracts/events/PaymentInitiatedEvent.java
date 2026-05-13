package com.randmteam2.tripplanning.contracts.events;
import java.math.BigDecimal;
public record PaymentInitiatedEvent(Long settlementId, Long itineraryId, BigDecimal amount) {}
