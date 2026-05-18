package com.randmteam2.tripplanning.contracts.dto;

import java.io.Serializable;

public record ItinerarySummaryDTO(Long itineraryId, Long destinationId, Long userId, String status) implements Serializable {}
