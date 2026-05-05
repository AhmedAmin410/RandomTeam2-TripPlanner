package com.randmteam2.tripplanning.booking.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "activity_audit_trail")
public class ActivityEvent implements MongoEvent {

    @Id
    private String id;
    private String action;
    private LocalDateTime timestamp;
    private Long activityId;
    private Long itineraryId;
    private String category;
    private Map<String, Object> details;

    public String getId()                         { return id; }
    public void setId(String id)                  { this.id = id; }
    public String getAction()                     { return action; }
    public void setAction(String action)          { this.action = action; }
    public LocalDateTime getTimestamp()           { return timestamp; }
    public void setTimestamp(LocalDateTime t)     { this.timestamp = t; }
    public Long getActivityId()                   { return activityId; }
    public void setActivityId(Long activityId)    { this.activityId = activityId; }
    public Long getItineraryId()                  { return itineraryId; }
    public void setItineraryId(Long itineraryId)  { this.itineraryId = itineraryId; }
    public String getCategory()                   { return category; }
    public void setCategory(String category)      { this.category = category; }
    public Map<String, Object> getDetails()       { return details; }
    public void setDetails(Map<String, Object> d) { this.details = d; }
}
