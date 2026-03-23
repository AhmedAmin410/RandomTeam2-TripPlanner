package com.team2.tripplanning.booking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCoupon {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Coupon coupon;

    @Column(nullable = false)
    private Double discountApplied;

    @Column(nullable = false)
    private LocalDateTime appliedAt = LocalDateTime.now();
    
    
}
