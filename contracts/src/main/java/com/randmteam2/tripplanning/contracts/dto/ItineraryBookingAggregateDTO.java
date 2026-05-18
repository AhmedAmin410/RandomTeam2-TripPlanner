package com.randmteam2.tripplanning.contracts.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public record ItineraryBookingAggregateDTO(
    List<Long> itineraryIds,
    Long totalBookings,
    BigDecimal totalRevenue,
    BigDecimal averageBookingAmount
) implements Serializable {}

