package com.randmteam2.tripplanning.booking.dto;

import java.util.Map;

public record UserBookingSummaryDTO(
        Long userId,
        Integer totalBookings,
        Double totalAmount,
        Map<String, Double> typeBreakdown
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private Integer totalBookings;
        private Double totalAmount;
        private Map<String, Double> typeBreakdown;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder totalBookings(Integer totalBookings) {
            this.totalBookings = totalBookings;
            return this;
        }

        public Builder totalAmount(Double totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder typeBreakdown(Map<String, Double> typeBreakdown) {
            this.typeBreakdown = typeBreakdown;
            return this;
        }

        public UserBookingSummaryDTO build() {
            return new UserBookingSummaryDTO(userId, totalBookings, totalAmount, typeBreakdown);
        }
    }
}
