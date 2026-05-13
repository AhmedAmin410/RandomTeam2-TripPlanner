package com.randmteam2.tripplanning.contracts.events;
public record PaymentFailedEvent(Long settlementId, Long itineraryId, String reason) {}
