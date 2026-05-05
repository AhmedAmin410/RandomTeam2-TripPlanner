package com.randmteam2.tripplanning.booking.strategy;

import com.randmteam2.tripplanning.booking.model.Booking;

public interface RefundStrategy {
    RefundResult calculateRefund(Booking booking);
}