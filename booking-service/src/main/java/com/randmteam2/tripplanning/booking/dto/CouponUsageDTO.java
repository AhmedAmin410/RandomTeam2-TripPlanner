package com.randmteam2.tripplanning.booking.dto;

import java.time.LocalDateTime;

public class CouponUsageDTO {

    private Long couponId;
    private String code;
    private String discountType;
    private Double discountValue;
    private Long timesUsed;
    private Double totalDiscountGiven;
    private Boolean active;
    private Boolean expired;

    public CouponUsageDTO() {}

    public CouponUsageDTO(Long couponId, String code, String discountType, Double discountValue,
                           Long timesUsed, Double totalDiscountGiven, Boolean active,
                           LocalDateTime expiryDate) {
        this.couponId = couponId;
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.timesUsed = timesUsed;
        this.totalDiscountGiven = totalDiscountGiven;
        this.active = active;
        this.expired = expiryDate.isBefore(LocalDateTime.now());
    }

    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public Double getDiscountValue() { return discountValue; }
    public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }

    public Long getTimesUsed() { return timesUsed; }
    public void setTimesUsed(Long timesUsed) { this.timesUsed = timesUsed; }

    public Double getTotalDiscountGiven() { return totalDiscountGiven; }
    public void setTotalDiscountGiven(Double totalDiscountGiven) { this.totalDiscountGiven = totalDiscountGiven; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getExpired() { return expired; }
    public void setExpired(Boolean expired) { this.expired = expired; }
}
