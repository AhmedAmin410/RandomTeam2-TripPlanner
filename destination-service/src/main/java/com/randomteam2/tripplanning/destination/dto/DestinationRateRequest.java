package com.randomteam2.tripplanning.destination.dto;

public class DestinationRateRequest {

    private Long itineraryId;
    private Integer rating;

    public Long getItineraryId() {
        return itineraryId;
    }

    public void setItineraryId(Long itineraryId) {
        this.itineraryId = itineraryId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
