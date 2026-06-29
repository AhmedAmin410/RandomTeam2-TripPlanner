package com.randmteam2.tripplanning.contracts.dto;

import java.io.Serializable;

public record UserBookingTotalDTO(Long userId, Double totalAmount, Long tripCount) implements Serializable {}
