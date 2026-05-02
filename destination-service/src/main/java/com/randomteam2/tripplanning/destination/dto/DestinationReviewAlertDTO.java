package com.randomteam2.tripplanning.destination.dto;

import com.randomteam2.tripplanning.destination.model.DestinationReview;

import java.util.List;

public class DestinationReviewAlertDTO {

    private Long destinationId;
    private String destinationName;
    private String destinationStatus;
    private List<DestinationReview> lowRatedReviews;
    private int lowRatedCount;

    public DestinationReviewAlertDTO() {}

    private DestinationReviewAlertDTO(Builder builder) {
        this.destinationId = builder.destinationId;
        this.destinationName = builder.destinationName;
        this.destinationStatus = builder.destinationStatus;
        this.lowRatedReviews = builder.lowRatedReviews;
        this.lowRatedCount = builder.lowRatedCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long destinationId;
        private String destinationName;
        private String destinationStatus;
        private List<DestinationReview> lowRatedReviews;
        private int lowRatedCount;

        public Builder destinationId(Long destinationId) { this.destinationId = destinationId; return this; }
        public Builder destinationName(String destinationName) { this.destinationName = destinationName; return this; }
        public Builder destinationStatus(String destinationStatus) { this.destinationStatus = destinationStatus; return this; }
        public Builder lowRatedReviews(List<DestinationReview> lowRatedReviews) { this.lowRatedReviews = lowRatedReviews; return this; }
        public Builder lowRatedCount(int lowRatedCount) { this.lowRatedCount = lowRatedCount; return this; }
        public DestinationReviewAlertDTO build() { return new DestinationReviewAlertDTO(this); }
    }

    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long destinationId) { this.destinationId = destinationId; }
    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }
    public String getDestinationStatus() { return destinationStatus; }
    public void setDestinationStatus(String destinationStatus) { this.destinationStatus = destinationStatus; }
    public List<DestinationReview> getLowRatedReviews() { return lowRatedReviews; }
    public void setLowRatedReviews(List<DestinationReview> lowRatedReviews) { this.lowRatedReviews = lowRatedReviews; }
    public int getLowRatedCount() { return lowRatedCount; }
    public void setLowRatedCount(int lowRatedCount) { this.lowRatedCount = lowRatedCount; }
}
