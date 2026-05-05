package com.randmteam2.tripplanning.itinerary.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("User")
public class UserNode {

    @Id
    private Long userId;

    @Property("name")
    private String name;

    @Relationship(type = "VISITED", direction = Relationship.Direction.OUTGOING)
    private List<VisitedRelationship> visited = new ArrayList<>();

    public UserNode() {}

    public UserNode(Long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<VisitedRelationship> getVisited() { return visited; }
    public void setVisited(List<VisitedRelationship> visited) { this.visited = visited; }
}
