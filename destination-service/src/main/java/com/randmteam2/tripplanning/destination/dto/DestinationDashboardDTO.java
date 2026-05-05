
//Dp-7: This class implements the Adapter Pattern (DP-7) for Elasticsearch hits to a DestinationDashboardDTO.
package com.randomteam2.tripplanning.destination.dto;

import java.util.Map;

public class DestinationDashboardDTO {

    private Long destinationId;
    private String destinationName;
    private Double totalRevenue;
    private Map<String, Double> revenueBySeason;

    private DestinationDashboardDTO() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long destinationId;
        private String destinationName;
        private Double totalRevenue;
        private Map<String, Double> revenueBySeason;

        public Builder destinationId(Long destinationId) {
            this.destinationId = destinationId;
            return this;
        }

        public Builder destinationName(String destinationName) {
            this.destinationName = destinationName;
            return this;
        }

        public Builder totalRevenue(Double totalRevenue) {
            this.totalRevenue = totalRevenue;
            return this;
        }

        public Builder revenueBySeason(Map<String, Double> revenueBySeason) {
            this.revenueBySeason = revenueBySeason;
            return this;
        }

        public DestinationDashboardDTO build() {
            DestinationDashboardDTO dto = new DestinationDashboardDTO();
            dto.destinationId = this.destinationId;
            dto.destinationName = this.destinationName;
            dto.totalRevenue = this.totalRevenue;
            dto.revenueBySeason = this.revenueBySeason;
            return dto;
        }
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public Double getTotalRevenue() {
        return totalRevenue;
    }

    public Map<String, Double> getRevenueBySeason() {
        return revenueBySeason;
    }
}