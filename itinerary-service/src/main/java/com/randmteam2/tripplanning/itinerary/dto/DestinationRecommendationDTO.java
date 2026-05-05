package com.randmteam2.tripplanning.itinerary.dto;

public class DestinationRecommendationDTO {
    private Long destinationId;
    private String name;
    private String country;
    private String category;
    private Long score;

    public DestinationRecommendationDTO() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final DestinationRecommendationDTO dto = new DestinationRecommendationDTO();
        public Builder destinationId(Long v) { dto.destinationId = v; return this; }
        public Builder name(String v) { dto.name = v; return this; }
        public Builder country(String v) { dto.country = v; return this; }
        public Builder category(String v) { dto.category = v; return this; }
        public Builder score(Long v) { dto.score = v; return this; }
        public DestinationRecommendationDTO build() { return dto; }
    }

    public DestinationRecommendationDTO(Long destinationId, String name, String country, String category, Long score) {
        this.destinationId = destinationId;
        this.name = name;
        this.country = country;
        this.category = category;
        this.score = score;
    }

    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long destinationId) { this.destinationId = destinationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getScore() { return score; }
    public void setScore(Long score) { this.score = score; }
}
