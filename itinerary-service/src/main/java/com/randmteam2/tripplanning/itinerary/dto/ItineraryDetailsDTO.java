package com.randmteam2.tripplanning.itinerary.dto;

import com.randmteam2.tripplanning.itinerary.model.ItineraryDay;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ItineraryDetailsDTO {
    private Long itineraryId;
    private Long userId;
    private Long destinationId;
    private String title;
    private String status;
    private Double estimatedBudget;
    private Map<String, Object> metadata;
    private List<ItineraryDay> days;
    private int totalDays;
    private long completedDays;


    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long destinationId) { this.destinationId = destinationId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getEstimatedBudget() { return estimatedBudget; }
    public void setEstimatedBudget(Double estimatedBudget) { this.estimatedBudget = estimatedBudget; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public List<ItineraryDay> getDays() { return days; }
    public void setDays(List<ItineraryDay> days) { this.days = days; }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public long getCompletedDays() { return completedDays; }
    public void setCompletedDays(long completedDays) { this.completedDays = completedDays; }
}