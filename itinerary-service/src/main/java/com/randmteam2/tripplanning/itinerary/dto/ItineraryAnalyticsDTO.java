package com.randmteam2.tripplanning.itinerary.dto;

public class ItineraryAnalyticsDTO {
    private Long totalItineraries;
    private Long completedItineraries;
    private Long cancelledItineraries;
    private Double totalBudget;
    private Double averageBudget;
    private Double completionRate;

    public Long getTotalItineraries() { return totalItineraries; }
    public void setTotalItineraries(Long totalItineraries) { this.totalItineraries = totalItineraries; }

    public Long getCompletedItineraries() { return completedItineraries; }
    public void setCompletedItineraries(Long completedItineraries) { this.completedItineraries = completedItineraries; }

    public Long getCancelledItineraries() { return cancelledItineraries; }
    public void setCancelledItineraries(Long cancelledItineraries) { this.cancelledItineraries = cancelledItineraries; }

    public Double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(Double totalBudget) { this.totalBudget = totalBudget; }

    public Double getAverageBudget() { return averageBudget; }
    public void setAverageBudget(Double averageBudget) { this.averageBudget = averageBudget; }

    public Double getCompletionRate() { return completionRate; }
    public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }
}