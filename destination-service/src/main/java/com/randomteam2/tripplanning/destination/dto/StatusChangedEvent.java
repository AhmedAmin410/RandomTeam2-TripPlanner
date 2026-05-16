package com.randomteam2.tripplanning.destination.dto;

public record StatusChangedEvent(
        Long destinationId,
        String oldStatus,
        String newStatus
) {}
