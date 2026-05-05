package com.randmteam2.tripplanning.activity.adapter;

import com.randmteam2.tripplanning.activity.dto.ActivityAnalyticsDTO;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Adapter Pattern (DP-8): adapts a MongoDB Document to ActivityAnalyticsDTO.
 */
@Component
public class MongoDocumentAdapter {

    public ActivityAnalyticsDTO adapt(Document doc) {
        return ActivityAnalyticsDTO.builder()
                .totalActivities(doc.getLong("totalActivities"))
                .averageCost(doc.getDouble("averageCost"))
                .averageDurationHours(doc.getDouble("averageDurationHours"))
                .activitiesByCategory(doc.get("activitiesByCategory", java.util.Map.class))
                .build();
    }
}