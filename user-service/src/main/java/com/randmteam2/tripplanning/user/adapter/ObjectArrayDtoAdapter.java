package com.randmteam2.tripplanning.user.adapter;

import com.randmteam2.tripplanning.user.dto.UserTripSummaryAggregateDTO;
import com.randmteam2.tripplanning.user.dto.UserTripSummaryDTO;
import org.springframework.stereotype.Component;

@Component
public class ObjectArrayDtoAdapter {

    public UserTripSummaryDTO adapt(UserTripSummaryAggregateDTO aggregate, Long userId, String name) {
        return UserTripSummaryDTO.builder()
                .userId(userId)
                .name(name)
                .totalTrips(aggregate.getTotalTrips() != null ? aggregate.getTotalTrips() : 0L)
                .completedTrips(aggregate.getCompletedTrips() != null ? aggregate.getCompletedTrips() : 0L)
                .cancelledTrips(aggregate.getCancelledTrips() != null ? aggregate.getCancelledTrips() : 0L)
                .totalBudget(aggregate.getTotalBudget() != null ? aggregate.getTotalBudget() : 0.0)
                .averageBudget(aggregate.getAverageBudget() != null ? aggregate.getAverageBudget() : 0.0)
                .build();
    }
}
