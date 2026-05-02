package com.randmteam2.tripplanning.itinerary.neo4j;

import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RelationshipProperties
public class VisitedRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private DestinationNode destination;

    private Integer visitCount;
    private LocalDateTime lastVisitDate;
    private List<Long> recordedItineraryIds = new ArrayList<>();

    public VisitedRelationship() {}

    public VisitedRelationship(DestinationNode destination) {
        this.destination = destination;
        this.visitCount = 0;
        this.recordedItineraryIds = new ArrayList<>();
    }

    public Long getId() { return id; }
    public DestinationNode getDestination() { return destination; }
    public void setDestination(DestinationNode destination) { this.destination = destination; }
    public Integer getVisitCount() { return visitCount; }
    public void setVisitCount(Integer visitCount) { this.visitCount = visitCount; }
    public LocalDateTime getLastVisitDate() { return lastVisitDate; }
    public void setLastVisitDate(LocalDateTime lastVisitDate) { this.lastVisitDate = lastVisitDate; }
    public List<Long> getRecordedItineraryIds() { return recordedItineraryIds; }
    public void setRecordedItineraryIds(List<Long> recordedItineraryIds) { this.recordedItineraryIds = recordedItineraryIds; }
}