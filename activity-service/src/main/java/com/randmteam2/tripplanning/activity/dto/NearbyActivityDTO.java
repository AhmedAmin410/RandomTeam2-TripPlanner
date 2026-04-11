package com.randmteam2.tripplanning.activity.dto;

public record NearbyActivityDTO(
        Long activityId,
        String name,
        String category,
        Double lat,
        Double lon,
        Double distanceKm
) {}