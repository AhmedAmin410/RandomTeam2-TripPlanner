package com.randmteam2.tripplanning.itinerary.dto;

import java.math.BigDecimal;
import java.util.List;

public class BookingAggregateDTO {

    private List<Long> itineraryIds;
    private Long totalBookings;
    private BigDecimal totalRevenue;
    private BigDecimal averageBookingAmount;

    public BookingAggregateDTO() {}

    public List<Long> getItineraryIds() { return itineraryIds; }
    public void setItineraryIds(List<Long> itineraryIds) { this.itineraryIds = itineraryIds; }

    public Long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(Long totalBookings) { this.totalBookings = totalBookings; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getAverageBookingAmount() { return averageBookingAmount; }
    public void setAverageBookingAmount(BigDecimal averageBookingAmount) { this.averageBookingAmount = averageBookingAmount; }
}