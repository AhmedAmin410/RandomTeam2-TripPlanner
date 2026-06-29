package com.randomteam2.tripplanning.destination.dto;

public record DestinationRatedEvent(
        Long destinationId,
        Long itineraryId,
        Double rating,
        Long userId
) {}
