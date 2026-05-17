import java.io.Serializable;
package com.randmteam2.tripplanning.contracts.dto;
public record UserTripSummaryAggregateDTO(
    Long totalTrips,
    Long completedTrips,
    Long cancelledTrips,
    Double totalBudget,
    Double averageBudget
) implements Serializable {}
