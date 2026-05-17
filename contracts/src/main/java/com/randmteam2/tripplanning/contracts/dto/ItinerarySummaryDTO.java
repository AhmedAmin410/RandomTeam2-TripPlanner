import java.io.Serializable;
package com.randmteam2.tripplanning.contracts.dto;
public record ItinerarySummaryDTO(Long itineraryId, Long destinationId, Long userId, String status) implements Serializable {}
