package com.randmteam2.tripplanning.itinerary.dto;

import com.randmteam2.tripplanning.itinerary.model.ItineraryDay;
import java.util.List;
import java.util.Map;

public record ItineraryDetailsDTO(
        Long itineraryId,
        Long userId,
        Long destinationId,
        String title,
        String status,
        Double estimatedBudget,
        Map<String, Object> metadata,
        List<ItineraryDay> days,
        int totalDays,
        long completedDays
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long itineraryId;
        private Long userId;
        private Long destinationId;
        private String title;
        private String status;
        private Double estimatedBudget;
        private Map<String, Object> metadata;
        private List<ItineraryDay> days;
        private int totalDays;
        private long completedDays;

        public Builder itineraryId(Long itineraryId) {
            this.itineraryId = itineraryId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder destinationId(Long destinationId) {
            this.destinationId = destinationId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder estimatedBudget(Double estimatedBudget) {
            this.estimatedBudget = estimatedBudget;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder days(List<ItineraryDay> days) {
            this.days = days;
            return this;
        }

        public Builder totalDays(int totalDays) {
            this.totalDays = totalDays;
            return this;
        }

        public Builder completedDays(long completedDays) {
            this.completedDays = completedDays;
            return this;
        }

        public ItineraryDetailsDTO build() {
            return new ItineraryDetailsDTO(
                    itineraryId, userId, destinationId, title, status,
                    estimatedBudget, metadata, days, totalDays, completedDays
            );
        }
    }
}