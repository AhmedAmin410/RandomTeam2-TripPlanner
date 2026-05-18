package com.randmteam2.tripplanning.contracts.dto;

import java.io.Serializable;

public record DestinationSummaryDTO(Long destinationId, String name, String country, String category) implements Serializable {}
