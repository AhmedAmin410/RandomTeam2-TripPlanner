package com.randmteam2.tripplanning.booking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_coupons")
public class BookingCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discount_applied", nullable = false)
    private Double discountApplied;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    @JsonIgnore
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "coupon_id")
    @JsonIgnore
    private Coupon coupon;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Double getDiscountApplied() { return discountApplied; }
    public void setDiscountApplied(Double discountApplied) { this.discountApplied = discountApplied; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public Coupon getCoupon() { return coupon; }
    public void setCoupon(Coupon coupon) { this.coupon = coupon; }
}