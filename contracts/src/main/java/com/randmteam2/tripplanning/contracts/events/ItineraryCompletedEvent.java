package com.randmteam2.tripplanning.contracts.events;
import java.math.BigDecimal;
public record ItineraryCompletedEvent(Long itineraryId, Long userId, Long destinationId, BigDecimal totalAmount) {}
