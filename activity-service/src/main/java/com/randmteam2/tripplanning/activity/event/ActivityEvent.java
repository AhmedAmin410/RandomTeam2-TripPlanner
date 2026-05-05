package com.randmteam2.tripplanning.activity.event;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "activity_events")
public class ActivityEvent implements MongoEvent {
    @Id private String id;
    private Long activityId;
    private String action;
    private String details;
    private LocalDateTime timestamp;

    public ActivityEvent() {}
    public ActivityEvent(Long activityId, String action, String details) {
        this.activityId = activityId; this.action = action; this.details = details; this.timestamp = LocalDateTime.now();
    }
    @Override public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    @Override public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    @Override public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    @Override public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
