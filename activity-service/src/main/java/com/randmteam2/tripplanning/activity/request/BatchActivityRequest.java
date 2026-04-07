package com.randmteam2.tripplanning.activity.request;

import java.util.List;

public class BatchActivityRequest {

    private Long itineraryId;
    private List<ActivityRequest> activities;

    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }

    public List<ActivityRequest> getActivities() { return activities; }
    public void setActivities(List<ActivityRequest> activities) { this.activities = activities; }
}