package com.randmteam2.tripplanning.booking.dto;

public record RevenueReportDTO(
        Double totalRevenue,
        Integer totalBookings,
        Double averageBookingAmount,
        Double cancelledAmount,
        Integer cancelledCount
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Double totalRevenue;
        private Integer totalBookings;
        private Double averageBookingAmount;
        private Double cancelledAmount;
        private Integer cancelledCount;

        public Builder totalRevenue(Double totalRevenue) {
            this.totalRevenue = totalRevenue;
            return this;
        }

        public Builder totalBookings(Integer totalBookings) {
            this.totalBookings = totalBookings;
            return this;
        }

        public Builder averageBookingAmount(Double averageBookingAmount) {
            this.averageBookingAmount = averageBookingAmount;
            return this;
        }

        public Builder cancelledAmount(Double cancelledAmount) {
            this.cancelledAmount = cancelledAmount;
            return this;
        }

        public Builder cancelledCount(Integer cancelledCount) {
            this.cancelledCount = cancelledCount;
            return this;
        }

        public RevenueReportDTO build() {
            return new RevenueReportDTO(
                    totalRevenue,
                    totalBookings,
                    averageBookingAmount,
                    cancelledAmount,
                    cancelledCount
            );
        }
    }
}
