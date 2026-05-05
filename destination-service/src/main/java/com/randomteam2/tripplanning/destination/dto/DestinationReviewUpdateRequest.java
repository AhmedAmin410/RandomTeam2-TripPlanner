package com.randomteam2.tripplanning.destination.dto;

import com.randomteam2.tripplanning.destination.model.DestinationReviewType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.Map;

/**
 * Partial update for a destination review; only non-null fields are applied.
 */
public class DestinationReviewUpdateRequest {

    private DestinationReviewType type;

    private String content;

    @Min(1)
    @Max(5)
    private Integer rating;

    private LocalDate visitDate;

    private Boolean verified;

    private Map<String, Object> metadata;

    public DestinationReviewType getType() {
        return type;
    }

    public void setType(DestinationReviewType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
