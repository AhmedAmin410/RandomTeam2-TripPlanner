package com.randmteam2.tripplanning.booking.dto;

public class RevenueReportDTO {

    private double totalRevenue;
    private long totalBookings;
    private double averageBookingAmount;
    private double cancelledAmount;
    private long cancelledCount;

    public RevenueReportDTO() {}

    public RevenueReportDTO(double totalRevenue, long totalBookings, double averageBookingAmount,
                            double cancelledAmount, long cancelledCount) {
        this.totalRevenue = totalRevenue;
        this.totalBookings = totalBookings;
        this.averageBookingAmount = averageBookingAmount;
        this.cancelledAmount = cancelledAmount;
        this.cancelledCount = cancelledCount;
    }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public double getAverageBookingAmount() { return averageBookingAmount; }
    public void setAverageBookingAmount(double averageBookingAmount) { this.averageBookingAmount = averageBookingAmount; }

    public double getCancelledAmount() { return cancelledAmount; }
    public void setCancelledAmount(double cancelledAmount) { this.cancelledAmount = cancelledAmount; }

    public long getCancelledCount() { return cancelledCount; }
    public void setCancelledCount(long cancelledCount) { this.cancelledCount = cancelledCount; }
}
