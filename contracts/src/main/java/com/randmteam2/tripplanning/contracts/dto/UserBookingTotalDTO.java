package com.randmteam2.tripplanning.contracts.dto;

import java.math.BigDecimal;

public record UserBookingTotalDTO(Long userId, BigDecimal totalAmount, Integer tripCount) {}