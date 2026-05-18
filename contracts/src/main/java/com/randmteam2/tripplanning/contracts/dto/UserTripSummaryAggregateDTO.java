package com.randmteam2.tripplanning.contracts.dto;

import java.io.Serializable;

public record UserTripSummaryAggregateDTO(
    Long totalTrips,
    Long completedTrips,
    Long cancelledTrips,
    Double totalBudget,
    Double averageBudget
) implements Serializable {}
