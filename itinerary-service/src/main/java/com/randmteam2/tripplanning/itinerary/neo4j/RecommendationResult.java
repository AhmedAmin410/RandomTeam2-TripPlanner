// neo4j/RecommendationResult.java
package com.randmteam2.tripplanning.itinerary.neo4j;

public interface RecommendationResult {
    Long getDestinationId();
    Long getScore();
}