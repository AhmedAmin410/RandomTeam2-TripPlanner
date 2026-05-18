package com.randmteam2.tripplanning.booking.repository;

import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    List<Booking> findByItineraryIdAndStatus(Long itineraryId, BookingStatus status);

    // S5-READ-DB: Standard Spring Data queries for confirmed bookings isolation
    List<Booking> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, BookingStatus status);

    List<Booking> findByItineraryIdAndStatusOrderByCreatedAtDesc(Long itineraryId, BookingStatus status);

    long countByItineraryIdAndStatus(Long itineraryId, BookingStatus status);

    @Query("""
        select coalesce(sum(b.amount), 0)
        from Booking b
        where b.itineraryId = :itineraryId and b.status = com.randmteam2.tripplanning.booking.model.BookingStatus.CONFIRMED
        """)
    Double sumConfirmedAmountByItineraryId(@Param("itineraryId") Long itineraryId);

    @Query("""
        select coalesce(sum(b.amount), 0)
        from Booking b
        where b.userId = :userId
          and b.status = com.randmteam2.tripplanning.booking.model.BookingStatus.CONFIRMED
          and b.createdAt between :startDate and :endDate
        """)
    Double sumConfirmedAmountByUserAndDateRange(@Param("userId") Long userId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Query("""
        select count(b)
        from Booking b
        where b.userId = :userId
          and b.status = com.randmteam2.tripplanning.booking.model.BookingStatus.CONFIRMED
          and b.createdAt between :startDate and :endDate
        """)
    Long countConfirmedTripsByUserAndDateRange(@Param("userId") Long userId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    @Query("""
        select coalesce(sum(b.amount), 0)
        from Booking b
        where b.itineraryId in :itineraryIds
          and b.status = :status
          and b.createdAt between :startDate and :endDate
        """)
    Double sumAmountByItineraryIdsAndStatus(@Param("itineraryIds") List<Long> itineraryIds,
                                            @Param("status") BookingStatus status,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    @Query("""
        select count(b)
        from Booking b
        where b.itineraryId in :itineraryIds
          and b.status = :status
          and b.createdAt between :startDate and :endDate
        """)
    Long countByItineraryIdsAndStatus(@Param("itineraryIds") List<Long> itineraryIds,
                                      @Param("status") BookingStatus status,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    @Modifying
    @Query("""
        update Booking b
        set b.status = :target
        where b.id = :bookingId and b.status = :source
        """)
    int transitionBookingStatus(@Param("bookingId") Long bookingId,
                                @Param("source") BookingStatus source,
                                @Param("target") BookingStatus target);

    @Modifying
    @Query("""
        update Booking b
        set b.status = :target
        where b.itineraryId = :itineraryId and b.status = :source
        """)
    int transitionBookingsForItinerary(@Param("itineraryId") Long itineraryId,
                                       @Param("source") BookingStatus source,
                                       @Param("target") BookingStatus target);

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

    // S5-F10 confirmed bookings in date range (local table only)
    @Query("""
        select b from Booking b
        where b.status = com.randmteam2.tripplanning.booking.model.BookingStatus.CONFIRMED
          and b.createdAt between :startDate and :endDate
        """)
    List<Booking> findConfirmedBookingsInRange(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

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
}