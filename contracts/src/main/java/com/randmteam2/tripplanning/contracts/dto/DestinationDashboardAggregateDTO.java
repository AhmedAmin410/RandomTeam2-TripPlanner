package com.randmteam2.tripplanning.contracts.dto;

import java.io.Serializable;

public record DestinationDashboardAggregateDTO(
    Long totalItineraries,
    Long completedItineraries,
    Long totalVisitors
) implements Serializable {}
