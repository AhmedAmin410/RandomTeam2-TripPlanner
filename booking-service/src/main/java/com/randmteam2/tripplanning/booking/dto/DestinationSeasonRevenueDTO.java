package com.randmteam2.tripplanning.booking.dto;

public class DestinationSeasonRevenueDTO {
    private Long   destinationId;
    private String destinationName;
    private Double totalRevenue;
    private Double baseRevenue;
    private Double surchargeRevenue;
    private Long   peakBookingCount;
    private Long   offPeakBookingCount;

    private DestinationSeasonRevenueDTO() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final DestinationSeasonRevenueDTO dto = new DestinationSeasonRevenueDTO();
        public Builder destinationId(Long v)       { dto.destinationId = v;       return this; }
        public Builder destinationName(String v)   { dto.destinationName = v;     return this; }
        public Builder totalRevenue(Double v)      { dto.totalRevenue = v;        return this; }
        public Builder baseRevenue(Double v)       { dto.baseRevenue = v;         return this; }
        public Builder surchargeRevenue(Double v)  { dto.surchargeRevenue = v;    return this; }
        public Builder peakBookingCount(Long v)    { dto.peakBookingCount = v;    return this; }
        public Builder offPeakBookingCount(Long v) { dto.offPeakBookingCount = v; return this; }
        public DestinationSeasonRevenueDTO build() { return dto; }
    }

    public Long   getDestinationId()       { return destinationId; }
    public String getDestinationName()     { return destinationName; }
    public Double getTotalRevenue()        { return totalRevenue; }
    public Double getBaseRevenue()         { return baseRevenue; }
    public Double getSurchargeRevenue()    { return surchargeRevenue; }
    public Long   getPeakBookingCount()    { return peakBookingCount; }
    public Long   getOffPeakBookingCount() { return offPeakBookingCount; }
}