package com.randomteam2.tripplanning.destination.dto;

import com.randomteam2.tripplanning.destination.model.DestinationReview;
import com.randomteam2.tripplanning.destination.model.DestinationReviewType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * API view of a review without embedding the full {@link com.randomteam2.tripplanning.destination.model.Destination}.
 */
public class DestinationReviewResponse {

    private Long id;
    private Long destinationId;
    private DestinationReviewType type;
    private String content;
    private Integer rating;
    private LocalDate visitDate;
    private Boolean verified;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;

    public static DestinationReviewResponse fromEntity(DestinationReview entity) {
        if (entity == null) {
            return null;
        }
        DestinationReviewResponse dto = new DestinationReviewResponse();
        dto.setId(entity.getId());
        if (entity.getDestination() != null) {
            dto.setDestinationId(entity.getDestination().getId());
        }
        dto.setType(entity.getType());
        dto.setContent(entity.getContent());
        dto.setRating(entity.getRating());
        dto.setVisitDate(entity.getVisitDate());
        dto.setVerified(entity.getVerified());
        dto.setMetadata(entity.getMetadata());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
