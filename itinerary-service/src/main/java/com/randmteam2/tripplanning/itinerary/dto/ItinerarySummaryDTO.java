package com.randmteam2.tripplanning.itinerary.dto;

public class ItinerarySummaryDTO {
    private Long itineraryId;
    private Long destinationId;
    private Long userId;
    private String status;

    public ItinerarySummaryDTO(Long itineraryId, Long destinationId, Long userId, String status) {
        this.itineraryId = itineraryId;
        this.destinationId = destinationId;
        this.userId = userId;
        this.status = status;
    }

    public Long getItineraryId() { return itineraryId; }
    public Long getDestinationId() { return destinationId; }
    public Long getUserId() { return userId; }
    public String getStatus() { return status; }
}