package com.randmteam2.tripplanning.user.dto;

public class UserBookingTotalDTO {

    private Long userId;
    private Double totalAmount;
    private Long tripCount;

    public UserBookingTotalDTO() {}

    public Long getUserId() { return userId; }
    public Double getTotalAmount() { return totalAmount; }
    public Long getTripCount() { return tripCount; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public void setTripCount(Long tripCount) { this.tripCount = tripCount; }
}
