package com.randmteam2.tripplanning.activity.dto;

import java.time.LocalDateTime;

public class BudgetActivityDTO {
    private Long activityId; // Spec rename
    private String name;
    private String category;
    private Double cost;
    private Double latitude;
    private Double longitude;
    private LocalDateTime scheduledTime;

    public BudgetActivityDTO(Long activityId, String name, String category, Double cost,
                             Double latitude, Double longitude, LocalDateTime scheduledTime) {
        this.activityId = activityId;
        this.name = name;
        this.category = category;
        this.cost = cost;
        this.latitude = latitude;
        this.longitude = longitude;
        this.scheduledTime = scheduledTime;
    }

    // Getters
    public Long getActivityId() { return activityId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public Double getCost() { return cost; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
}