package com.randomteam2.tripplanning.destination.dto;

public record ItineraryDTO(
        Long id,
        Long destinationId,
        Long userId,
        String status
) {}
