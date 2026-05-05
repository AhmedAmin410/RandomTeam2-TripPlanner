package com.randmteam2.tripplanning.booking.strategy;

import com.randmteam2.tripplanning.booking.model.Booking;

public class LateCancellationRefundStrategy implements RefundStrategy {
    @Override
    public RefundResult calculateRefund(Booking booking) {
        return new RefundResult(booking.getAmount() * 0.25, "LATE", "LATE_REFUND");
    }
}