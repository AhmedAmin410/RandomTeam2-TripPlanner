package com.randmteam2.tripplanning.activity.dto;

import com.randmteam2.tripplanning.activity.model.Activity;

public class NearbyActivityDTO {
    private Long activityId;
    private String name;
    private String category;
    private Double lat;
    private Double lon;
    private Double distanceKm;

    // Standard constructor (for manual mapping)
    public NearbyActivityDTO(Long activityId, String name, String category, Double lat, Double lon, Double distanceKm) {
        this.activityId = activityId;
        this.name = name;
        this.category = category;
        this.lat = lat;
        this.lon = lon;
        this.distanceKm = distanceKm;
    }

    // Overloaded constructor to handle the Enum automatically
    public NearbyActivityDTO(Long activityId, String name, Activity.Category category, Double lat, Double lon, Double distanceKm) {
        this.activityId = activityId;
        this.name = name;
        this.category = category != null ? category.name() : null; // Fixes the red error internally
        this.lat = lat;
        this.lon = lon;
        this.distanceKm = distanceKm;
    }

    // Getters
    public Long getActivityId() { return activityId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public Double getLat() { return lat; }
    public Double getLon() { return lon; }
    public Double getDistanceKm() { return distanceKm; }
}