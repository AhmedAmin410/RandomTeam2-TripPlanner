package com.randmteam2.tripplanning.activity.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long itineraryId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "activity_category")
    private Category category;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime scheduledTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    public enum Category {
        SIGHTSEEING, ADVENTURE, DINING, CULTURAL, LEISURE
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}