package com.randomteam2.tripplanning.destination.dto;

/**
 * Response body for S2-F3 GET /api/destinations/{id}/revenue.
 *
 * Always constructed via the Builder (M2 Builder retrofit contract preserved).
 */
public class DestinationRevenueDTO {

    private Long destinationId;
    private String name;
    private Long totalBookings;
    private Double totalRevenue;
    private Double averageBookingAmount;

    /** Required for Jackson / Redis deserialisation. */
    public DestinationRevenueDTO() {}

    private DestinationRevenueDTO(Builder b) {
        this.destinationId        = b.destinationId;
        this.name                 = b.name;
        this.totalBookings        = b.totalBookings;
        this.totalRevenue         = b.totalRevenue;
        this.averageBookingAmount = b.averageBookingAmount;
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

        public Builder destinationId(Long destinationId)               { this.destinationId = destinationId; return this; }
        public Builder name(String name)                               { this.name = name; return this; }
        public Builder totalBookings(Long totalBookings)               { this.totalBookings = totalBookings; return this; }
        public Builder totalRevenue(Double totalRevenue)               { this.totalRevenue = totalRevenue; return this; }
        public Builder averageBookingAmount(Double averageBookingAmount){ this.averageBookingAmount = averageBookingAmount; return this; }

        public DestinationRevenueDTO build() {
            return new DestinationRevenueDTO(this);
        }
    }

    // ── Getters & setters ────────────────────────────────────────────────────

    public Long getDestinationId()                  { return destinationId; }
    public void setDestinationId(Long v)            { this.destinationId = v; }

    public String getName()                         { return name; }
    public void setName(String v)                   { this.name = v; }

    public Long getTotalBookings()                  { return totalBookings; }
    public void setTotalBookings(Long v)            { this.totalBookings = v; }

    public Double getTotalRevenue()                 { return totalRevenue; }
    public void setTotalRevenue(Double v)           { this.totalRevenue = v; }

    public Double getAverageBookingAmount()         { return averageBookingAmount; }
    public void setAverageBookingAmount(Double v)   { this.averageBookingAmount = v; }
}