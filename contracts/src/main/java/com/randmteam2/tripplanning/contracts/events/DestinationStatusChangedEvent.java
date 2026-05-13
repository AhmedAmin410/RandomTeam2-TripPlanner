package com.randmteam2.tripplanning.contracts.events;
public record DestinationStatusChangedEvent(Long destinationId, String oldStatus, String newStatus) {}
