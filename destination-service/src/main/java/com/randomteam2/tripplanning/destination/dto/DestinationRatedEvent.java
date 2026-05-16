package com.randomteam2.tripplanning.destination.dto;

import java.time.LocalDateTime;

public record DestinationRatedEvent(
        Long destinationId,
        Long itineraryId,
        Integer rating,
        Long userId,
        LocalDateTime occurredAt
) {}
