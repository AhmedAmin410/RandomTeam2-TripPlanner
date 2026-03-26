package com.randmteam2.tripplanning.booking.repository;

import com.randmteam2.tripplanning.booking.model.BookingCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingCouponRepository extends JpaRepository<BookingCoupon, Long> {

    List<BookingCoupon> findByBookingId(Long bookingId);
}
