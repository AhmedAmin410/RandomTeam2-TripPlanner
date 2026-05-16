package com.randomteam2.tripplanning.destination.dto;

public record DestinationSummaryDTO(
        Long destinationId,
        String name,
        String country,
        String category
) {}
