package com.randmteam2.tripplanning.activity.dto;

import java.time.Instant;
import java.util.UUID;

public record ActivityEventDTO(
        UUID eventId,
        Long activityId,
        String status,
        Instant timestamp
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID eventId;
        private Long activityId;
        private String status;
        private Instant timestamp;

        public Builder eventId(UUID eventId) { this.eventId = eventId; return this; }
        public Builder activityId(Long activityId) { this.activityId = activityId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public ActivityEventDTO build() {
            return new ActivityEventDTO(eventId, activityId, status, timestamp);
        }
    }
}
