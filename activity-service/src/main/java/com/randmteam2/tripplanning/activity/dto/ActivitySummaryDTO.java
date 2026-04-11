package com.randmteam2.tripplanning.activity.dto;

public record ActivitySummaryDTO(
        Long itineraryId,
        Long totalActivities,
        Double averageCost,
        Double maxCost,
        java.time.LocalDateTime firstScheduledTime,
        java.time.LocalDateTime lastScheduledTime
) {}