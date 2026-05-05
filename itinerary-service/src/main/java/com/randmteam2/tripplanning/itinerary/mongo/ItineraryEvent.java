package com.randmteam2.tripplanning.itinerary.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "itinerary_events")
public class ItineraryEvent implements MongoEvent {

    @Id
    private String id;
    private Long itineraryId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    public ItineraryEvent() {}

    public ItineraryEvent(Long itineraryId, String action,
                          LocalDateTime timestamp, Map<String, Object> details) {
        this.itineraryId = itineraryId;
        this.action = action;
        this.timestamp = timestamp;
        this.details = details;
    }

    @Override public String getId() { return id; }
    @Override public LocalDateTime getTimestamp() { return timestamp; }
    @Override public String getAction() { return action; }
    @Override public Map<String, Object> getDetails() { return details; }

    public Long getItineraryId() { return itineraryId; }
    public void setId(String id) { this.id = id; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }
    public void setAction(String action) { this.action = action; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
