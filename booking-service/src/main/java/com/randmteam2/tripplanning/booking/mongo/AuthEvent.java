package com.randmteam2.tripplanning.booking.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "auth_audit_trail")
public class AuthEvent implements MongoEvent {

    @Id
    private String id;
    private String action;
    private LocalDateTime timestamp;
    private Long userId;
    private String username;
    private String ipAddress;
    private Map<String, Object> details;

    public String getId()                         { return id; }
    public void setId(String id)                  { this.id = id; }
    public String getAction()                     { return action; }
    public void setAction(String action)          { this.action = action; }
    public LocalDateTime getTimestamp()           { return timestamp; }
    public void setTimestamp(LocalDateTime t)     { this.timestamp = t; }
    public Long getUserId()                       { return userId; }
    public void setUserId(Long userId)            { this.userId = userId; }
    public String getUsername()                   { return username; }
    public void setUsername(String username)      { this.username = username; }
    public String getIpAddress()                  { return ipAddress; }
    public void setIpAddress(String ipAddress)    { this.ipAddress = ipAddress; }
    public Map<String, Object> getDetails()       { return details; }
    public void setDetails(Map<String, Object> d) { this.details = d; }
}
