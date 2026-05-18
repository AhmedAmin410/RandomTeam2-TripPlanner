package com.randmteam2.tripplanning.contracts.dto;

import java.io.Serializable;
import java.util.List;

public record BatchDestinationRequest(List<Long> destinationIds) implements Serializable {}
