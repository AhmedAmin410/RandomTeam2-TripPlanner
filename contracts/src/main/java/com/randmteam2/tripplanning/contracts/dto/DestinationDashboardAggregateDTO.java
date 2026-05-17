import java.io.Serializable;
package com.randmteam2.tripplanning.contracts.dto;
public record DestinationDashboardAggregateDTO(
    Long totalItineraries,
    Long completedItineraries,
    Long totalVisitors
) implements Serializable {}
