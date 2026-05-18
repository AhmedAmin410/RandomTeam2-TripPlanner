package com.randmteam2.tripplanning.contracts.dto;

import java.math.BigDecimal;
import java.util.List;
public record ItineraryBookingAggregateDTO(Long totalBookings, Double totalRevenue) {

    public ItineraryBookingAggregateDTO(List<Long> itineraryIds,
                                        Long totalBookings,
                                        BigDecimal totalRevenue,
                                        BigDecimal averageAmount) {

        this(totalBookings, totalRevenue.doubleValue());
    }
}