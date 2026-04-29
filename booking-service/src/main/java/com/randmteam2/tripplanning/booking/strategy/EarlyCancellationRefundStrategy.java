package com.randmteam2.tripplanning.booking.strategy;

import com.randmteam2.tripplanning.booking.model.Booking;

public class EarlyCancellationRefundStrategy implements RefundStrategy {
    @Override
    public RefundResult calculateRefund(Booking booking) {
        return new RefundResult(booking.getAmount(), "EARLY", "FULL_REFUND");
    }
}