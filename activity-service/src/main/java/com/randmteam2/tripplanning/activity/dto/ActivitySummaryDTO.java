package com.randmteam2.tripplanning.activity.dto;

import java.time.LocalDateTime;

public record ActivitySummaryDTO(
        Long itineraryId,
        Long totalActivities,
        Double averageCost,
        Double maxCost,
        LocalDateTime firstScheduledTime,
        LocalDateTime lastScheduledTime
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long itineraryId;
        private Long totalActivities;
        private Double averageCost;
        private Double maxCost;
        private LocalDateTime firstScheduledTime;
        private LocalDateTime lastScheduledTime;

        public Builder itineraryId(Long itineraryId) { this.itineraryId = itineraryId; return this; }
        public Builder totalActivities(Long totalActivities) { this.totalActivities = totalActivities; return this; }
        public Builder averageCost(Double averageCost) { this.averageCost = averageCost; return this; }
        public Builder maxCost(Double maxCost) { this.maxCost = maxCost; return this; }
        public Builder firstScheduledTime(LocalDateTime firstScheduledTime) { this.firstScheduledTime = firstScheduledTime; return this; }
        public Builder lastScheduledTime(LocalDateTime lastScheduledTime) { this.lastScheduledTime = lastScheduledTime; return this; }

        public ActivitySummaryDTO build() {
            return new ActivitySummaryDTO(itineraryId, totalActivities, averageCost, maxCost, firstScheduledTime, lastScheduledTime);
        }
    }
}