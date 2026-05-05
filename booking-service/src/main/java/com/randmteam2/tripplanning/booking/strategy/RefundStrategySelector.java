package com.randmteam2.tripplanning.booking.strategy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RefundStrategySelector {

    public RefundStrategy select(LocalDate itineraryStartDate, boolean itineraryStarted) {
        if (itineraryStarted) return new NoRefundStrategy();

        long days = ChronoUnit.DAYS.between(LocalDate.now(), itineraryStartDate);

        if (days <= 0)  return new NoRefundStrategy();
        if (days > 14)  return new EarlyCancellationRefundStrategy();
        if (days >= 7)  return new MidCancellationRefundStrategy();
        return new LateCancellationRefundStrategy();
    }
}