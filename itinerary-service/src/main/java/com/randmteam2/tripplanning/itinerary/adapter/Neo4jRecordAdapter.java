package com.randmteam2.tripplanning.itinerary.adapter;

import com.randmteam2.tripplanning.itinerary.neo4j.DestinationNode;
import com.randmteam2.tripplanning.itinerary.neo4j.UserNode;
import com.randmteam2.tripplanning.itinerary.neo4j.VisitedRelationship;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class Neo4jRecordAdapter {

    public Map<String, Object> adapt(UserNode userNode) {
        return adaptUserNode(userNode);
    }

    public Map<String, Object> adaptUserNode(UserNode userNode) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userNode.getUserId());
        result.put("name", userNode.getName());
        List<Map<String, Object>> visits = userNode.getVisited().stream()
                .map(this::adaptVisitedRelationship)
                .collect(Collectors.toList());
        result.put("visited", visits);
        return result;
    }

    public Map<String, Object> adaptVisitedRelationship(VisitedRelationship rel) {
        Map<String, Object> result = new HashMap<>();
        result.put("visitCount", rel.getVisitCount());
        result.put("lastVisitDate", rel.getLastVisitDate());
        result.put("recordedItineraryIds", rel.getRecordedItineraryIds());
        if (rel.getDestination() != null) {
            result.put("destination", adaptDestinationNode(rel.getDestination()));
        }
        return result;
    }

    public Map<String, Object> adaptDestinationNode(DestinationNode destinationNode) {
        Map<String, Object> result = new HashMap<>();
        result.put("destinationId", destinationNode.getDestinationId());
        result.put("name", destinationNode.getName());
        result.put("country", destinationNode.getCountry());
        result.put("category", destinationNode.getCategory());
        return result;
    }
}
