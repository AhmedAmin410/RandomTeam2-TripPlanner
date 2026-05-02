package com.randmteam2.tripplanning.activity.dto;

import java.time.LocalDateTime;

public record BudgetActivityDTO(
        Long activityId,
        String name,
        String category,
        Double cost,
        Double latitude,
        Double longitude,
        LocalDateTime scheduledTime
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long activityId;
        private String name;
        private String category;
        private Double cost;
        private Double latitude;
        private Double longitude;
        private LocalDateTime scheduledTime;

        public Builder activityId(Long activityId) { this.activityId = activityId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder cost(Double cost) { this.cost = cost; return this; }
        public Builder latitude(Double latitude) { this.latitude = latitude; return this; }
        public Builder longitude(Double longitude) { this.longitude = longitude; return this; }
        public Builder scheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; return this; }

        public BudgetActivityDTO build() {
            return new BudgetActivityDTO(activityId, name, category, cost, latitude, longitude, scheduledTime);
        }
    }
}