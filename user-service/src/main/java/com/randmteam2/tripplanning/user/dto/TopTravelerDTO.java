package com.randmteam2.tripplanning.user.dto;

public record TopTravelerDTO(
        Long userId,
        String name,
        Double totalSpent,
        Long tripCount
) {}