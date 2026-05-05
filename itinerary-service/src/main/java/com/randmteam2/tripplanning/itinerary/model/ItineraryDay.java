package com.randmteam2.tripplanning.itinerary.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "itinerary_days")
public class ItineraryDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer dayOrder;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @ManyToOne
    @JoinColumn(name = "itinerary_id")
    @JsonBackReference
    private Itinerary itinerary;

    public enum Status {
        PLANNED, COMPLETED, SKIPPED
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getDayOrder() { return dayOrder; }
    public void setDayOrder(Integer dayOrder) { this.dayOrder = dayOrder; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Itinerary getItinerary() { return itinerary; }
    public void setItinerary(Itinerary itinerary) { this.itinerary = itinerary; }
}
