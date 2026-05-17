package com.randmteam2.tripplanning.itinerary.dto;

public class UserTripSummaryAggregateDTO {
    private long totalTrips;
    private long completedTrips;
    private long cancelledTrips;
    private Double totalBudget;
    private Double averageBudget;

    public UserTripSummaryAggregateDTO(long totalTrips, long completedTrips,
                                       long cancelledTrips, Double totalBudget, Double averageBudget) {
        this.totalTrips = totalTrips;
        this.completedTrips = completedTrips;
        this.cancelledTrips = cancelledTrips;
        this.totalBudget = totalBudget;
        this.averageBudget = averageBudget;
    }

    public long getTotalTrips() { return totalTrips; }
    public long getCompletedTrips() { return completedTrips; }
    public long getCancelledTrips() { return cancelledTrips; }
    public Double getTotalBudget() { return totalBudget; }
    public Double getAverageBudget() { return averageBudget; }
}