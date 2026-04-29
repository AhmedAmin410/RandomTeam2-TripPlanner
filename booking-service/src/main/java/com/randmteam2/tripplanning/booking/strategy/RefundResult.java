package com.randmteam2.tripplanning.booking.strategy;

public class RefundResult {
    private final double refundAmount;
    private final String tier;
    private final String reasonCode;

    public RefundResult(double refundAmount, String tier, String reasonCode) {
        this.refundAmount = refundAmount;
        this.tier = tier;
        this.reasonCode = reasonCode;
    }

    public double getRefundAmount() { return refundAmount; }
    public String getTier()         { return tier; }
    public String getReasonCode()   { return reasonCode; }
}