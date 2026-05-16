package com.randomteam2.tripplanning.destination.adapter;

import com.randomteam2.tripplanning.destination.dto.DestinationBookingRevenueAggregateDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationRevenueDTO;
import org.springframework.stereotype.Component;

/**
 * Adapter Pattern: converts booking revenue aggregates
 * into a DestinationRevenueDTO (used by S2-F3).
 */
@Component
public class ObjectArrayDtoAdapter {

    public DestinationRevenueDTO adaptRevenue(Long destinationId, String name,
                                              DestinationBookingRevenueAggregateDTO aggregate) {
        if (aggregate == null) {
            return adaptRevenue(destinationId, name, (Object[]) null);
        }
        double totalRevenue = aggregate.totalRevenue() != null ? aggregate.totalRevenue().doubleValue() : 0.0;
        double avgAmount = aggregate.averageBookingAmount() != null
                ? aggregate.averageBookingAmount().doubleValue() : 0.0;
        long totalBookings = aggregate.totalBookings() != null ? aggregate.totalBookings() : 0L;
        return DestinationRevenueDTO.builder()
                .destinationId(destinationId)
                .name(name)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue)
                .averageBookingAmount(avgAmount)
                .build();
    }

    public DestinationRevenueDTO adaptRevenue(Long destinationId, String name, Object[] row) {
        long totalBookings = row != null && row[0] != null ? ((Number) row[0]).longValue() : 0L;
        double totalRevenue = row != null && row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        double avgAmount = row != null && row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

        return DestinationRevenueDTO.builder()
                .destinationId(destinationId)
                .name(name)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue)
                .averageBookingAmount(avgAmount)
                .build();
    }
}
