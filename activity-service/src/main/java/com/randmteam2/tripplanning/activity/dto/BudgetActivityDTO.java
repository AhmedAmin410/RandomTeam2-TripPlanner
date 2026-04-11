package com.randmteam2.tripplanning.activity.dto;

public record BudgetActivityDTO(
        Long activityId,
        String name,
        String category,
        Double cost,
        Double latitude,
        Double longitude,
        java.time.LocalDateTime scheduledTime
) {}