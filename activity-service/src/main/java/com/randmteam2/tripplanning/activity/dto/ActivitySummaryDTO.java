package com.randmteam2.tripplanning.activity.dto;

import java.time.LocalDateTime;

public class ActivitySummaryDTO {
    private Long itineraryId;
    private Long totalActivities;
    private Double averageCost;
    private Double maxCost;
    private LocalDateTime firstScheduledTime;
    private LocalDateTime lastScheduledTime;

    public ActivitySummaryDTO(Long itineraryId, Long totalActivities, Double averageCost,
                              Double maxCost, LocalDateTime firstScheduledTime, LocalDateTime lastScheduledTime) {
        this.itineraryId = itineraryId;
        this.totalActivities = totalActivities;
        this.averageCost = averageCost;
        this.maxCost = maxCost;
        this.firstScheduledTime = firstScheduledTime;
        this.lastScheduledTime = lastScheduledTime;
    }

    // Getters
    public Long getItineraryId() { return itineraryId; }
    public Long getTotalActivities() { return totalActivities; }
    public Double getAverageCost() { return averageCost; }
    public Double getMaxCost() { return maxCost; }
    public LocalDateTime getFirstScheduledTime() { return firstScheduledTime; }
    public LocalDateTime getLastScheduledTime() { return lastScheduledTime; }
}
