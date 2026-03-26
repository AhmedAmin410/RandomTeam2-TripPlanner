package com.randmteam2.tripplanning.booking.repository;

import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.model.BookingCoupon;
import com.randmteam2.tripplanning.booking.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingCouponRepository extends JpaRepository<BookingCoupon, Long> {

    List<BookingCoupon> findByBookingId(Long bookingId);

    Optional<BookingCoupon> findByBookingAndCoupon(Booking booking, Coupon coupon);
}
