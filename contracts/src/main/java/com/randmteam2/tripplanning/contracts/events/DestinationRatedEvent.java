package com.randmteam2.tripplanning.contracts.events;
public record DestinationRatedEvent(Long destinationId, Long itineraryId, Double rating, Long userId) {}
