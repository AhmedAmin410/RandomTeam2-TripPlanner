package com.randmteam2.tripplanning.itinerary.dto;

public class TripCostEstimateDTO {
    private Double estimatedAccommodation;
    private Double estimatedTransport;
    private Double estimatedActivities;
    private Double estimatedTotal;
    private Double seasonMultiplier;

    public Double getEstimatedAccommodation() { return estimatedAccommodation; }
    public void setEstimatedAccommodation(Double estimatedAccommodation) { this.estimatedAccommodation = estimatedAccommodation; }

    public Double getEstimatedTransport() { return estimatedTransport; }
    public void setEstimatedTransport(Double estimatedTransport) { this.estimatedTransport = estimatedTransport; }

    public Double getEstimatedActivities() { return estimatedActivities; }
    public void setEstimatedActivities(Double estimatedActivities) { this.estimatedActivities = estimatedActivities; }

    public Double getEstimatedTotal() { return estimatedTotal; }
    public void setEstimatedTotal(Double estimatedTotal) { this.estimatedTotal = estimatedTotal; }

    public Double getSeasonMultiplier() { return seasonMultiplier; }
    public void setSeasonMultiplier(Double seasonMultiplier) { this.seasonMultiplier = seasonMultiplier; }
}