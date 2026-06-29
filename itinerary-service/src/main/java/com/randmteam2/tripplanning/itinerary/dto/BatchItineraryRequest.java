package com.randmteam2.tripplanning.itinerary.dto;

import java.util.List;

public class BatchItineraryRequest {
    private List<Long> itineraryIds;

    public BatchItineraryRequest() {}

    public BatchItineraryRequest(List<Long> itineraryIds) {
        this.itineraryIds = itineraryIds;
    }

    public List<Long> getItineraryIds() { return itineraryIds; }
    public void setItineraryIds(List<Long> itineraryIds) { this.itineraryIds = itineraryIds; }
}