package com.randmteam2.tripplanning.contracts.dto;
import java.io.Serializable;
public record ConfirmedSummaryDTO(Long count, Double totalRevenue) implements Serializable {}
