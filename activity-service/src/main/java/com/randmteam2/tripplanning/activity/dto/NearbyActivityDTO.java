package com.randmteam2.tripplanning.activity.dto;

public record NearbyActivityDTO(
        Long activityId,
        String name,
        String category,
        Double lat,
        Double lon,
        Double distanceKm
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long activityId;
        private String name;
        private String category;
        private Double lat;
        private Double lon;
        private Double distanceKm;

        public Builder activityId(Long activityId) { this.activityId = activityId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder lat(Double lat) { this.lat = lat; return this; }
        public Builder lon(Double lon) { this.lon = lon; return this; }
        public Builder distanceKm(Double distanceKm) { this.distanceKm = distanceKm; return this; }

        public NearbyActivityDTO build() {
            return new NearbyActivityDTO(activityId, name, category, lat, lon, distanceKm);
        }
    }
}