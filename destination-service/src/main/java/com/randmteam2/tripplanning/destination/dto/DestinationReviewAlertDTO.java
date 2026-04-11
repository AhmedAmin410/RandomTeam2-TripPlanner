package com.randmteam2.tripplanning.destination.dto;

import com.randmteam2.tripplanning.destination.model.DestinationReview;

import java.util.List;

public class DestinationReviewAlertDTO {

    private Long destinationId;
    private String destinationName;
    private String destinationStatus;
    private List<DestinationReview> lowRatedReviews;
    private int lowRatedCount;

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public String getDestinationStatus() {
        return destinationStatus;
    }

    public void setDestinationStatus(String destinationStatus) {
        this.destinationStatus = destinationStatus;
    }

    public List<DestinationReview> getLowRatedReviews() {
        return lowRatedReviews;
    }

    public void setLowRatedReviews(List<DestinationReview> lowRatedReviews) {
        this.lowRatedReviews = lowRatedReviews;
    }

    public int getLowRatedCount() {
        return lowRatedCount;
    }

    public void setLowRatedCount(int lowRatedCount) {
        this.lowRatedCount = lowRatedCount;
    }
}
