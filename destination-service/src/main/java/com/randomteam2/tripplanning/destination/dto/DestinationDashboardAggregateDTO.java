package com.randomteam2.tripplanning.destination.dto;

public record DestinationDashboardAggregateDTO(
        Long totalItineraries,
        Long completedItineraries,
        Long totalVisitors
) {}
