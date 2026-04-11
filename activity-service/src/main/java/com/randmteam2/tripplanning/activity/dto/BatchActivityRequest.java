package com.randmteam2.tripplanning.activity.dto;

import com.randmteam2.tripplanning.activity.model.Activity;
import java.util.List;

public class BatchActivityRequest {

    private Long itineraryId;
    private List<Activity> activities;

    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }

    public List<Activity> getActivities() { return activities; }
    public void setActivities(List<Activity> activities) { this.activities = activities; }
}
