package com.randmteam2.tripplanning.booking.strategy;

import com.randmteam2.tripplanning.booking.model.Booking;

public class NoRefundStrategy implements RefundStrategy {
    @Override
    public RefundResult calculateRefund(Booking booking) {
        return new RefundResult(0.0, "NONE", "TRIP_ALREADY_STARTED");
    }
}