package com.randomteam2.tripplanning.destination.dto;

/**
 * Payload published to the destination.events exchange with routing key
 * "destination.status-changed" after a successful status transition (S2-F4).
 */
public record StatusChangedEvent(
        Long destinationId,
        String oldStatus,
        String newStatus
) {}