package com.randmteam2.tripplanning.user.dto;

public class UserTripSummaryDTO {

    private Long userId;
    private String name;
    private Long totalTrips;
    private Long completedTrips;
    private Long cancelledTrips;
    private Double totalSpent;
    private Double averageBudget;

    private UserTripSummaryDTO() {}

    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public Long getTotalTrips() { return totalTrips; }
    public Long getCompletedTrips() { return completedTrips; }
    public Long getCancelledTrips() { return cancelledTrips; }
    public Double getTotalSpent() { return totalSpent; }
    public Double getAverageBudget() { return averageBudget; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long userId;
        private String name;
        private Long totalTrips;
        private Long completedTrips;
        private Long cancelledTrips;
        private Double totalSpent;
        private Double averageBudget;

        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder totalTrips(Long totalTrips) { this.totalTrips = totalTrips; return this; }
        public Builder completedTrips(Long completedTrips) { this.completedTrips = completedTrips; return this; }
        public Builder cancelledTrips(Long cancelledTrips) { this.cancelledTrips = cancelledTrips; return this; }
        public Builder totalSpent(Double totalSpent) { this.totalSpent = totalSpent; return this; }
        public Builder averageBudget(Double averageBudget) { this.averageBudget = averageBudget; return this; }

        public UserTripSummaryDTO build() {
            UserTripSummaryDTO dto = new UserTripSummaryDTO();
            dto.userId = this.userId;
            dto.name = this.name;
            dto.totalTrips = this.totalTrips;
            dto.completedTrips = this.completedTrips;
            dto.cancelledTrips = this.cancelledTrips;
            dto.totalSpent = this.totalSpent;
            dto.averageBudget = this.averageBudget;
            return dto;
        }
    }
}
