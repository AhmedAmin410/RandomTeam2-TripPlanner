package com.randmteam2.tripplanning.contracts.events;
public record ItineraryCancelledEvent(Long itineraryId, Long userId, Long destinationId, String reason) {}
