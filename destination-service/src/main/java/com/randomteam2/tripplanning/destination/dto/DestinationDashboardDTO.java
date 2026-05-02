package com.randomteam2.tripplanning.destination.dto;

public class DestinationDashboardDTO {

    private Long destinationId;
    private String name;
    private Long totalItineraries;
    private Long completedItineraries;
    private Long totalVisitors;
    private Integer totalRatings;
    private Double averageRating;

    public DestinationDashboardDTO() {}

    private DestinationDashboardDTO(Builder builder) {
        this.destinationId = builder.destinationId;
        this.name = builder.name;
        this.totalItineraries = builder.totalItineraries;
        this.completedItineraries = builder.completedItineraries;
        this.totalVisitors = builder.totalVisitors;
        this.totalRatings = builder.totalRatings;
        this.averageRating = builder.averageRating;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long destinationId;
        private String name;
        private Long totalItineraries;
        private Long completedItineraries;
        private Long totalVisitors;
        private Integer totalRatings;
        private Double averageRating;

        public Builder destinationId(Long destinationId) { this.destinationId = destinationId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder totalItineraries(Long totalItineraries) { this.totalItineraries = totalItineraries; return this; }
        public Builder completedItineraries(Long completedItineraries) { this.completedItineraries = completedItineraries; return this; }
        public Builder totalVisitors(Long totalVisitors) { this.totalVisitors = totalVisitors; return this; }
        public Builder totalRatings(Integer totalRatings) { this.totalRatings = totalRatings; return this; }
        public Builder averageRating(Double averageRating) { this.averageRating = averageRating; return this; }
        public DestinationDashboardDTO build() { return new DestinationDashboardDTO(this); }
    }

    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long destinationId) { this.destinationId = destinationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getTotalItineraries() { return totalItineraries; }
    public void setTotalItineraries(Long totalItineraries) { this.totalItineraries = totalItineraries; }
    public Long getCompletedItineraries() { return completedItineraries; }
    public void setCompletedItineraries(Long completedItineraries) { this.completedItineraries = completedItineraries; }
    public Long getTotalVisitors() { return totalVisitors; }
    public void setTotalVisitors(Long totalVisitors) { this.totalVisitors = totalVisitors; }
    public Integer getTotalRatings() { return totalRatings; }
    public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
}
