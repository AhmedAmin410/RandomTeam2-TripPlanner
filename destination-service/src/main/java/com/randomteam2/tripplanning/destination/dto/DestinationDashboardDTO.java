package com.randomteam2.tripplanning.destination.dto;

/**
 * Response body for S2-F12 GET /api/destinations/{id}/dashboard.
 *
 * Built exclusively through the static Builder to preserve the M2 Builder retrofit contract.
 */
public class DestinationDashboardDTO {

    private Long destinationId;
    private String name;
    private Long totalItineraries;
    private Long completedItineraries;
    private Long totalVisitors;
    private Integer totalRatings;
    private Double averageRating;

    /** Required for Jackson / Redis deserialisation. */
    public DestinationDashboardDTO() {}

    private DestinationDashboardDTO(Builder b) {
        this.destinationId       = b.destinationId;
        this.name                = b.name;
        this.totalItineraries    = b.totalItineraries;
        this.completedItineraries = b.completedItineraries;
        this.totalVisitors       = b.totalVisitors;
        this.totalRatings        = b.totalRatings;
        this.averageRating       = b.averageRating;
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

        public Builder destinationId(Long destinationId)               { this.destinationId = destinationId; return this; }
        public Builder name(String name)                               { this.name = name; return this; }
        public Builder totalItineraries(Long totalItineraries)         { this.totalItineraries = totalItineraries; return this; }
        public Builder completedItineraries(Long completedItineraries) { this.completedItineraries = completedItineraries; return this; }
        public Builder totalVisitors(Long totalVisitors)               { this.totalVisitors = totalVisitors; return this; }
        public Builder totalRatings(Integer totalRatings)              { this.totalRatings = totalRatings; return this; }
        public Builder averageRating(Double averageRating)             { this.averageRating = averageRating; return this; }

        public DestinationDashboardDTO build() {
            return new DestinationDashboardDTO(this);
        }
    }

    // ── Getters & setters (Jackson + tests) ─────────────────────────────────

    public Long getDestinationId()              { return destinationId; }
    public void setDestinationId(Long v)        { this.destinationId = v; }

    public String getName()                     { return name; }
    public void setName(String v)               { this.name = v; }

    public Long getTotalItineraries()           { return totalItineraries; }
    public void setTotalItineraries(Long v)     { this.totalItineraries = v; }

    public Long getCompletedItineraries()       { return completedItineraries; }
    public void setCompletedItineraries(Long v) { this.completedItineraries = v; }

    public Long getTotalVisitors()              { return totalVisitors; }
    public void setTotalVisitors(Long v)        { this.totalVisitors = v; }

    public Integer getTotalRatings()            { return totalRatings; }
    public void setTotalRatings(Integer v)      { this.totalRatings = v; }

    public Double getAverageRating()            { return averageRating; }
    public void setAverageRating(Double v)      { this.averageRating = v; }
}