package com.randmteam2.tripplanning.user.dto;

public record TopTravelerDTO(
        Long userId,
        String name,
        Double totalSpent,
        Long tripCount
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long userId;
        private String name;
        private Double totalSpent;
        private Long tripCount;

        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder totalSpent(Double totalSpent) { this.totalSpent = totalSpent; return this; }
        public Builder tripCount(Long tripCount) { this.tripCount = tripCount; return this; }

        public TopTravelerDTO build() {
            return new TopTravelerDTO(userId, name, totalSpent, tripCount);
        }
    }
}
