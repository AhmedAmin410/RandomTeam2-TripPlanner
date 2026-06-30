package com.randmteam2.tripplanning.user.dto;

import java.util.Map;

public record SavedDestinationDTO(
        String label,
        String destinationName,
        String country,
        Double latitude,
        Double longitude,
        Boolean isDefault,
        Map<String, Object> metadata
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String label;
        private String destinationName;
        private String country;
        private Double latitude;
        private Double longitude;
        private Boolean isDefault;
        private Map<String, Object> metadata;

        public Builder label(String label) { this.label = label; return this; }
        public Builder destinationName(String destinationName) { this.destinationName = destinationName; return this; }
        public Builder country(String country) { this.country = country; return this; }
        public Builder latitude(Double latitude) { this.latitude = latitude; return this; }
        public Builder longitude(Double longitude) { this.longitude = longitude; return this; }
        public Builder isDefault(Boolean isDefault) { this.isDefault = isDefault; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }

        public SavedDestinationDTO build() {
            return new SavedDestinationDTO(label, destinationName, country, latitude, longitude, isDefault, metadata);
        }
    }
}
