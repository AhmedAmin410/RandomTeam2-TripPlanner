package com.randmteam2.tripplanning.booking.repository;

import com.randmteam2.tripplanning.booking.model.BookingCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingCouponRepository extends JpaRepository<BookingCoupon, Long> {

    List<BookingCoupon> findByBookingId(Long bookingId);

    // S5-F9: Most Used Coupons Report - native SQL
    @Query(value = """
            SELECT c.id AS couponId, c.code, c.discount_type AS discountType,
                   c.discount_value AS discountValue,
                   c.current_uses AS timesUsed,
                   COALESCE(SUM(bc.discount_applied), 0) AS totalDiscountGiven,
                   c.active, c.expiry_date AS expiryDate
            FROM coupons c
            JOIN booking_coupons bc ON c.id = bc.coupon_id
            GROUP BY c.id, c.code, c.discount_type, c.discount_value, c.current_uses, c.active, c.expiry_date
            ORDER BY timesUsed DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopUsedCoupons(@Param("limit") int limit);
}
