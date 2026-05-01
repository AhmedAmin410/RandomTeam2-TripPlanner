package com.randmteam2.tripplanning.user.adapter;

import com.randmteam2.tripplanning.user.dto.UserTripSummaryDTO;
import org.springframework.stereotype.Component;

@Component
public class ObjectArrayDtoAdapter {

    public UserTripSummaryDTO adapt(Object[] row, Long userId, String name) {
        Long totalTrips     = row[0] != null ? ((Number) row[0]).longValue()  : 0L;
        Long completedTrips = row[1] != null ? ((Number) row[1]).longValue()  : 0L;
        Long cancelledTrips = row[2] != null ? ((Number) row[2]).longValue()  : 0L;
        Double totalSpent   = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        Double avgBudget    = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
        return UserTripSummaryDTO.builder()
                .userId(userId)
                .name(name)
                .totalTrips(totalTrips)
                .completedTrips(completedTrips)
                .cancelledTrips(cancelledTrips)
                .totalSpent(totalSpent)
                .averageBudget(avgBudget)
                .build();
    }
}
