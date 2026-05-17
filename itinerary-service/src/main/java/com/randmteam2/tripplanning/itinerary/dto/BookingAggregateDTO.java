package com.randmteam2.tripplanning.itinerary.dto;

public class BookingAggregateDTO {
    private Long itineraryId;
    private Long totalBookings;
    private Double totalRevenue;
    private Double averageBookingAmount;

    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }

    public Long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(Long totalBookings) { this.totalBookings = totalBookings; }

    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

    public Double getAverageBookingAmount() { return averageBookingAmount; }
    public void setAverageBookingAmount(Double averageBookingAmount) { this.averageBookingAmount = averageBookingAmount; }
}