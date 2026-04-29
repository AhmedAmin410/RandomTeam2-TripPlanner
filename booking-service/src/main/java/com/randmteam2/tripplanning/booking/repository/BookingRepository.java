package com.randmteam2.tripplanning.booking.repository;

import com.randmteam2.tripplanning.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // S5-F1
    @Query(value = """
        SELECT * FROM bookings
        WHERE (:status IS NULL OR CAST(status AS text) = :status)
        AND (created_at IS NULL OR created_at BETWEEN :startDate AND :endDate)
        ORDER BY created_at DESC NULLS LAST
        """, nativeQuery = true)
    List<Booking> searchBookings(
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // S5-F3
    @Query(value = """
        SELECT type, COUNT(*) as count, COALESCE(SUM(amount), 0) as total
        FROM bookings
        WHERE user_id = :userId AND status = 'CONFIRMED'
        GROUP BY type
        """, nativeQuery = true)
    List<Object[]> getUserBookingSummary(@Param("userId") Long userId);

    // S5-F3 user check
    @Query(value = "SELECT id FROM users WHERE id = :userId", nativeQuery = true)
    List<Object[]> checkUserExists(@Param("userId") Long userId);

    // S5-F4
    @Query(value = "SELECT status FROM itineraries WHERE id = :id", nativeQuery = true)
    List<Object[]> findItineraryById(@Param("id") Long id);

    // S5-F6
    @Query(value = """
        SELECT status, COUNT(*) as count, COALESCE(SUM(amount), 0) as total
        FROM bookings
        WHERE (created_at IS NULL OR created_at BETWEEN :startDate AND :endDate)
        AND status IN ('CONFIRMED', 'CANCELLED')
        GROUP BY status
        """, nativeQuery = true)
    List<Object[]> getRevenueReport(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // S5-F9
    @Query(value = """
        SELECT c.id, c.code, c.discount_type::text, c.discount_value,
               c.current_uses, COALESCE(SUM(bc.discount_applied), 0),
               c.active, c.expiry_date
        FROM coupons c
        LEFT JOIN booking_coupons bc ON c.id = bc.coupon_id
        GROUP BY c.id
        ORDER BY c.current_uses DESC
        LIMIT :limit
        """, nativeQuery = true)

    List<Object[]> getTopUsedCoupons(@Param("limit") int limit);

    // MOD-BK1: get destination_id from an itinerary
    @Query(value = "SELECT destination_id FROM itineraries WHERE id = :itineraryId",
            nativeQuery = true)
    Long findDestinationIdByItineraryId(@Param("itineraryId") Long itineraryId);

    // MOD-BK1: count active itineraries for a destination
    @Query(value = """
    SELECT COUNT(*) FROM itineraries
    WHERE destination_id = :destinationId
    AND status IN ('PLANNED', 'IN_PROGRESS')
    """, nativeQuery = true)
    long countActiveItinerariesForDestination(@Param("destinationId") Long destinationId);
}