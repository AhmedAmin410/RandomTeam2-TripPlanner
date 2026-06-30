package com.randmteam2.tripplanning.activity.model;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("activity_lifecycle_events")
public class ActivityLifecycleEvent {

    @PrimaryKeyColumn(name = "\"activityId\"", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long activityId;

    @PrimaryKeyColumn(name = "timestamp", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Instant timestamp;

    @Column("event_id")
    private UUID eventId;

    @Column("status")
    private String status;

    public ActivityLifecycleEvent() {}

    public ActivityLifecycleEvent(Long activityId, Instant timestamp, UUID eventId, String status) {
        this.activityId = activityId;
        this.timestamp = timestamp;
        this.eventId = eventId;
        this.status = status;
    }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
