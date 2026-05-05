package com.randmteam2.tripplanning.booking.strategy;

import com.randmteam2.tripplanning.booking.model.Booking;

public class MidCancellationRefundStrategy implements RefundStrategy {
    @Override
    public RefundResult calculateRefund(Booking booking) {
        return new RefundResult(booking.getAmount() * 0.50, "MID", "PARTIAL_REFUND");
    }
}