package com.randmteam2.tripplanning.booking.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "destination_audit_trail")
public class DestinationEvent implements MongoEvent {

    @Id
    private String id;
    private String action;
    private LocalDateTime timestamp;
    private Long destinationId;
    private String destinationName;
    private String country;
    private Map<String, Object> details;

    public String getId()                                 { return id; }
    public void setId(String id)                          { this.id = id; }
    public String getAction()                             { return action; }
    public void setAction(String action)                  { this.action = action; }
    public LocalDateTime getTimestamp()                   { return timestamp; }
    public void setTimestamp(LocalDateTime t)             { this.timestamp = t; }
    public Long getDestinationId()                        { return destinationId; }
    public void setDestinationId(Long destinationId)      { this.destinationId = destinationId; }
    public String getDestinationName()                    { return destinationName; }
    public void setDestinationName(String destinationName){ this.destinationName = destinationName; }
    public String getCountry()                            { return country; }
    public void setCountry(String country)                { this.country = country; }
    public Map<String, Object> getDetails()               { return details; }
    public void setDetails(Map<String, Object> d)         { this.details = d; }
}
