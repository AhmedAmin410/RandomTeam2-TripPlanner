package com.randomteam2.tripplanning.destination.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Document(collection = "destination_events")
public class DestinationEvent implements MongoEvent {

    @Id
    private String id;

    private Long destinationId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details = new HashMap<>();

    public DestinationEvent() {
    }

    public DestinationEvent(Long destinationId, String action, LocalDateTime timestamp, Map<String, Object> details) {
        this.destinationId = destinationId;
        this.action = action;
        this.timestamp = timestamp;
        this.details = details == null ? new HashMap<>() : new HashMap<>(details);
    }

    @Override
    public String getId() {
        return id;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    @Override
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details == null ? new HashMap<>() : new HashMap<>(details);
    }
}