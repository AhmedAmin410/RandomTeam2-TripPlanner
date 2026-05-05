package com.randmteam2.tripplanning.itinerary.dto;

public record ItineraryAnalyticsDTO(
        Long totalItineraries,
        Long completedItineraries,
        Long cancelledItineraries,
        Double totalBudget,
        Double averageBudget,
        Double completionRate
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long totalItineraries;
        private Long completedItineraries;
        private Long cancelledItineraries;
        private Double totalBudget;
        private Double averageBudget;
        private Double completionRate;

        public Builder totalItineraries(Long totalItineraries) {
            this.totalItineraries = totalItineraries;
            return this;
        }

        public Builder completedItineraries(Long completedItineraries) {
            this.completedItineraries = completedItineraries;
            return this;
        }

        public Builder cancelledItineraries(Long cancelledItineraries) {
            this.cancelledItineraries = cancelledItineraries;
            return this;
        }

        public Builder totalBudget(Double totalBudget) {
            this.totalBudget = totalBudget;
            return this;
        }

        public Builder averageBudget(Double averageBudget) {
            this.averageBudget = averageBudget;
            return this;
        }

        public Builder completionRate(Double completionRate) {
            this.completionRate = completionRate;
            return this;
        }

        public ItineraryAnalyticsDTO build() {
            return new ItineraryAnalyticsDTO(
                    totalItineraries, completedItineraries, cancelledItineraries,
                    totalBudget, averageBudget, completionRate
            );
        }
    }
}
