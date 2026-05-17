package com.randmteam2.tripplanning.user.events;

import java.time.LocalDateTime;
import java.util.Map;

public class UserEventMessage {

    private String eventType;
    private String sourceService;
    private Long userId;
    private Long itineraryId;
    private String status;
    private String cacheKeyPattern;
    private LocalDateTime timestamp;
    private Map<String, Object> data;

    public UserEventMessage() {
    }

    public UserEventMessage(String eventType,
                            String sourceService,
                            Long userId,
                            Long itineraryId,
                            String status,
                            String cacheKeyPattern,
                            LocalDateTime timestamp,
                            Map<String, Object> data) {
        this.eventType = eventType;
        this.sourceService = sourceService;
        this.userId = userId;
        this.itineraryId = itineraryId;
        this.status = status;
        this.cacheKeyPattern = cacheKeyPattern;
        this.timestamp = timestamp;
        this.data = data;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSourceService() {
        return sourceService;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getItineraryId() {
        return itineraryId;
    }

    public String getStatus() {
        return status;
    }

    public String getCacheKeyPattern() {
        return cacheKeyPattern;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setItineraryId(Long itineraryId) {
        this.itineraryId = itineraryId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCacheKeyPattern(String cacheKeyPattern) {
        this.cacheKeyPattern = cacheKeyPattern;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}