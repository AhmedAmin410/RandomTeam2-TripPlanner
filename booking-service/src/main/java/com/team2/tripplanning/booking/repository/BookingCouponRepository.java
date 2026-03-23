package com.team2.tripplanning.booking.repository;

import com.team2.tripplanning.booking.model.BookingCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingCouponRepository extends JpaRepository<BookingCoupon, Long> {
    
}