package com.randmteam2.tripplanning.booking.dto;

import java.util.Map;

public class UserBookingSummaryDTO {

    private Long userId;
    private int totalBookings;
    private Double totalAmount;
    private Map<String, Double> typeBreakdown;

    public UserBookingSummaryDTO() {}

    public UserBookingSummaryDTO(Long userId, int totalBookings, Double totalAmount,
                                  Map<String, Double> typeBreakdown) {
        this.userId = userId;
        this.totalBookings = totalBookings;
        this.totalAmount = totalAmount;
        this.typeBreakdown = typeBreakdown;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public int getTotalBookings() { return totalBookings; }
    public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public Map<String, Double> getTypeBreakdown() { return typeBreakdown; }
    public void setTypeBreakdown(Map<String, Double> typeBreakdown) { this.typeBreakdown = typeBreakdown; }
}
