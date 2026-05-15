package com.randmteam2.tripplanning.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

// Mirrors the shape itinerary-service returns from GET /api/itineraries/user/{userId}/summary
public class UserTripSummaryAggregateDTO {

    @JsonAlias({"totalItineraries", "total_itineraries", "tripCount"})
    private Long totalTrips;

    @JsonAlias({"completedItineraries", "completed_itineraries"})
    private Long completedTrips;

    @JsonAlias({"cancelledItineraries", "cancelled_itineraries"})
    private Long cancelledTrips;

    private Double totalBudget;
    private Double averageBudget;

    public UserTripSummaryAggregateDTO() {}

    public Long getTotalTrips()      { return totalTrips; }
    public Long getCompletedTrips()  { return completedTrips; }
    public Long getCancelledTrips()  { return cancelledTrips; }
    public Double getTotalBudget()   { return totalBudget; }
    public Double getAverageBudget() { return averageBudget; }

    public void setTotalTrips(Long totalTrips)           { this.totalTrips = totalTrips; }
    public void setCompletedTrips(Long completedTrips)   { this.completedTrips = completedTrips; }
    public void setCancelledTrips(Long cancelledTrips)   { this.cancelledTrips = cancelledTrips; }
    public void setTotalBudget(Double totalBudget)       { this.totalBudget = totalBudget; }
    public void setAverageBudget(Double averageBudget)   { this.averageBudget = averageBudget; }

    public static UserTripSummaryAggregateDTO empty() {
        UserTripSummaryAggregateDTO dto = new UserTripSummaryAggregateDTO();
        dto.totalTrips = 0L;
        dto.completedTrips = 0L;
        dto.cancelledTrips = 0L;
        dto.totalBudget = 0.0;
        dto.averageBudget = 0.0;
        return dto;
    }
}
