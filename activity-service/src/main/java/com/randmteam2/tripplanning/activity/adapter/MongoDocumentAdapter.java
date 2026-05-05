// This class implements the Adapter Pattern (DP-7) for MongoDB documents.
// It converts MongoDB Document objects into ActivityEventDTO objects.
package com.randmteam2.tripplanning.activity.adapter;

import org.bson.Document;
import com.randmteam2.tripplanning.activity.dto.ActivityEventDTO;

public class MongoDocumentAdapter {

    public static ActivityEventDTO adapt(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        return ActivityEventDTO.builder()
                .eventId(document.get("eventId", java.util.UUID.class))
                .activityId(document.get("activityId", Long.class))
                .status(document.getString("status"))
                .timestamp(document.get("timestamp", java.time.Instant.class))
                .build();
    }
}