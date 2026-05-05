// This class implements the Adapter Pattern (DP-7) for Neo4j records to an ItineraryAnalyticsDTO.
// This is part of the requirement to implement Neo4jRecordAdapter for DP-7.
package com.randmteam2.tripplanning.itinerary.adapter;

import org.neo4j.driver.Record;
import com.randmteam2.tripplanning.itinerary.dto.ItineraryAnalyticsDTO;

public class Neo4jRecordAdapter {

    public static ItineraryAnalyticsDTO adapt(Record record) {
        if (record == null) {
            throw new IllegalArgumentException("Record cannot be null");
        }

        return ItineraryAnalyticsDTO.builder()
                .totalItineraries(record.get("totalItineraries").asLong())
                .completedItineraries(record.get("completedItineraries").asLong())
                .cancelledItineraries(record.get("cancelledItineraries").asLong())
                .totalBudget(record.get("totalBudget").asDouble())
                .averageBudget(record.get("averageBudget").asDouble())
                .completionRate(record.get("completionRate").asDouble())
                .build();
    }
}