package com.randmteam2.tripplanning.itinerary.dto;

public record TripCostEstimateDTO(
        Double estimatedAccommodation,
        Double estimatedTransport,
        Double estimatedActivities,
        Double estimatedTotal,
        Double seasonMultiplier
) {}