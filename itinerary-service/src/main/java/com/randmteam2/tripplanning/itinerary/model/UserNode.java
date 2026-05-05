package com.randmteam2.tripplanning.itinerary.model;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("User")
public class UserNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("userId")
    private Long userId;

    @Property("name")
    private String name;

    public UserNode() {}

    public UserNode(Long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
