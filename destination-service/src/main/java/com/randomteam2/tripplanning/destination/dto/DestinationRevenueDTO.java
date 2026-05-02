package com.randomteam2.tripplanning.destination.dto;

public class DestinationRevenueDTO {

    private Long destinationId;
    private String name;
    private Long totalBookings;
    private Double totalRevenue;
    private Double averageBookingAmount;

    public DestinationRevenueDTO() {}

    private DestinationRevenueDTO(Builder builder) {
        this.destinationId = builder.destinationId;
        this.name = builder.name;
        this.totalBookings = builder.totalBookings;
        this.totalRevenue = builder.totalRevenue;
        this.averageBookingAmount = builder.averageBookingAmount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long destinationId;
        private String name;
        private Long totalBookings;
        private Double totalRevenue;
        private Double averageBookingAmount;

        public Builder destinationId(Long destinationId) { this.destinationId = destinationId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder totalBookings(Long totalBookings) { this.totalBookings = totalBookings; return this; }
        public Builder totalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; return this; }
        public Builder averageBookingAmount(Double averageBookingAmount) { this.averageBookingAmount = averageBookingAmount; return this; }
        public DestinationRevenueDTO build() { return new DestinationRevenueDTO(this); }
    }

    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long destinationId) { this.destinationId = destinationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(Long totalBookings) { this.totalBookings = totalBookings; }
    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
    public Double getAverageBookingAmount() { return averageBookingAmount; }
    public void setAverageBookingAmount(Double averageBookingAmount) { this.averageBookingAmount = averageBookingAmount; }
}
