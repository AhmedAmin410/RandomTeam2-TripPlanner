package com.randmteam2.tripplanning.user.dto;

public class UserTripSummaryDTO {

    private Long userId;
    private String name;
    private Long totalTrips;
    private Long completedTrips;
    private Long cancelledTrips;
    private Double totalSpent;
    private Double averageBudget;

    public UserTripSummaryDTO(Long userId, String name, Long totalTrips,
                              Long completedTrips, Long cancelledTrips,
                              Double totalSpent, Double averageBudget) {
        this.userId = userId;
        this.name = name;
        this.totalTrips = totalTrips;
        this.completedTrips = completedTrips;
        this.cancelledTrips = cancelledTrips;
        this.totalSpent = totalSpent;
        this.averageBudget = averageBudget;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Long getTotalTrips() {
        return totalTrips;
    }

    public Long getCompletedTrips() {
        return completedTrips;
    }

    public Long getCancelledTrips() {
        return cancelledTrips;
    }

    public Double getTotalSpent() {
        return totalSpent;
    }

    public Double getAverageBudget() {
        return averageBudget;
    }
}