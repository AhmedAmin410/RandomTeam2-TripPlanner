package com.randmteam2.tripplanning.itinerary.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("Destination")
public class DestinationNode {

    @Id
    private Long destinationId;

    @Property("name")
    private String name;

    @Property("country")
    private String country;

    @Property("category")
    private String category;

    public DestinationNode() {}

    public DestinationNode(Long destinationId, String name, String country, String category) {
        this.destinationId = destinationId;
        this.name = name;
        this.country = country;
        this.category = category;
    }

    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long destinationId) { this.destinationId = destinationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}