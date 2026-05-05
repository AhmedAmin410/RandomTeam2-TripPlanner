package com.randmteam2.tripplanning.itinerary.dto;

public record TripCostRequestDTO(
        Long destinationId,
        Integer numberOfDays,
        Integer numberOfTravelers
) {}
