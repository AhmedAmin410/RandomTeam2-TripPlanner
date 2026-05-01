package com.randmteam2.tripplanning.itinerary.dto;

public record TripCostEstimateDTO(
        Double estimatedAccommodation,
        Double estimatedTransport,
        Double estimatedActivities,
        Double estimatedTotal,
        Double seasonMultiplier
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Double estimatedAccommodation;
        private Double estimatedTransport;
        private Double estimatedActivities;
        private Double estimatedTotal;
        private Double seasonMultiplier;

        public Builder estimatedAccommodation(Double estimatedAccommodation) {
            this.estimatedAccommodation = estimatedAccommodation;
            return this;
        }

        public Builder estimatedTransport(Double estimatedTransport) {
            this.estimatedTransport = estimatedTransport;
            return this;
        }

        public Builder estimatedActivities(Double estimatedActivities) {
            this.estimatedActivities = estimatedActivities;
            return this;
        }

        public Builder estimatedTotal(Double estimatedTotal) {
            this.estimatedTotal = estimatedTotal;
            return this;
        }

        public Builder seasonMultiplier(Double seasonMultiplier) {
            this.seasonMultiplier = seasonMultiplier;
            return this;
        }

        public TripCostEstimateDTO build() {
            return new TripCostEstimateDTO(
                    estimatedAccommodation, estimatedTransport, estimatedActivities,
                    estimatedTotal, seasonMultiplier
            );
        }
    }
}