import java.io.Serializable;
package com.randmteam2.tripplanning.contracts.dto;
public record ItineraryBookingAggregateDTO(Long totalBookings, Double totalRevenue) implements Serializable {}
