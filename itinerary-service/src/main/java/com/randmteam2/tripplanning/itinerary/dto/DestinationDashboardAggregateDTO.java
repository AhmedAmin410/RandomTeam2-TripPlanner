package com.randmteam2.tripplanning.itinerary.dto;

public record DestinationDashboardAggregateDTO(
        long totalItineraries,
        long completedItineraries,
        long totalVisitors
) {}