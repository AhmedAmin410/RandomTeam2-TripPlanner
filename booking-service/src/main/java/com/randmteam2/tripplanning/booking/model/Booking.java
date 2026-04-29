package com.randmteam2.tripplanning.booking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "itin_id", nullable = false)
    private Long itineraryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "booking_details", columnDefinition = "jsonb")
    private Map<String, Object> bookingDetails;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<BookingCoupon> bookingCoupons = new ArrayList<>();



    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public BookingType getType() { return type; }
    public void setType(BookingType type) { this.type = type; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public Map<String, Object> getBookingDetails() { return bookingDetails; }
    public void setBookingDetails(Map<String, Object> bookingDetails) { this.bookingDetails = bookingDetails; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<BookingCoupon> getBookingCoupons() { return bookingCoupons; }
    public void setBookingCoupons(List<BookingCoupon> bookingCoupons) { this.bookingCoupons = bookingCoupons; }
}