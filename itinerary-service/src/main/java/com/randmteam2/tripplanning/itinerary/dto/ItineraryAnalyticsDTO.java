package com.randmteam2.tripplanning.itinerary.dto;

public record ItineraryAnalyticsDTO(
        Long totalItineraries,
        Long completedItineraries,
        Long cancelledItineraries,
        Double totalBudget,
        Double averageBudget,
        Double completionRate
) {}