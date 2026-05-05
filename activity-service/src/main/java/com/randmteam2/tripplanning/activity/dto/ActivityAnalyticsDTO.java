package com.randmteam2.tripplanning.activity.dto;

import java.util.Map;

public class ActivityAnalyticsDTO {
    private Long totalActivities;
    private Double averageCost;
    private Double averageDurationHours;
    private Map<String, Long> activitiesByCategory;

    private ActivityAnalyticsDTO() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ActivityAnalyticsDTO dto = new ActivityAnalyticsDTO();

        public Builder totalActivities(Long v) { dto.totalActivities = v; return this; }
        public Builder averageCost(Double v) { dto.averageCost = v; return this; }
        public Builder averageDurationHours(Double v) { dto.averageDurationHours = v; return this; }
        public Builder activitiesByCategory(Map<String, Long> v) { dto.activitiesByCategory = v; return this; }
        public ActivityAnalyticsDTO build() { return dto; }
    }

    public Long getTotalActivities() { return totalActivities; }
    public Double getAverageCost() { return averageCost; }
    public Double getAverageDurationHours() { return averageDurationHours; }
    public Map<String, Long> getActivitiesByCategory() { return activitiesByCategory; }
}
