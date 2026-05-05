package com.randmteam2.tripplanning.itinerary.dto;

import java.util.Map;

public class ItineraryAnalyticsDashboardDTO {

    private long totalItineraries;
    private double totalBudget;
    private double averageBudget;
    private double completionRate;
    private Map<String, Long> itinerariesByStatus;

    private ItineraryAnalyticsDashboardDTO() {}

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private long totalItineraries;
        private double totalBudget;
        private double averageBudget;
        private double completionRate;
        private Map<String, Long> itinerariesByStatus;

        public Builder totalItineraries(long v) { this.totalItineraries = v; return this; }
        public Builder totalBudget(double v) { this.totalBudget = v; return this; }
        public Builder averageBudget(double v) { this.averageBudget = v; return this; }
        public Builder completionRate(double v) { this.completionRate = v; return this; }
        public Builder itinerariesByStatus(Map<String, Long> v) { this.itinerariesByStatus = v; return this; }

        public ItineraryAnalyticsDashboardDTO build() {
            ItineraryAnalyticsDashboardDTO dto = new ItineraryAnalyticsDashboardDTO();
            dto.totalItineraries = this.totalItineraries;
            dto.totalBudget = this.totalBudget;
            dto.averageBudget = this.averageBudget;
            dto.completionRate = this.completionRate;
            dto.itinerariesByStatus = this.itinerariesByStatus;
            return dto;
        }
    }

    public long getTotalItineraries() { return totalItineraries; }
    public double getTotalBudget() { return totalBudget; }
    public double getAverageBudget() { return averageBudget; }
    public double getCompletionRate() { return completionRate; }
    public Map<String, Long> getItinerariesByStatus() { return itinerariesByStatus; }
}
