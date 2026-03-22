package com.randmteam2.tripplanning.itinerary.repository;

import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    @Query(value = """
        SELECT COALESCE(SUM(b.amount), 0)
        FROM bookings b
        WHERE b.itinerary_id = :itineraryId
        AND b.status = 'CONFIRMED'
        """, nativeQuery = true)
    Double sumConfirmedBookings(@Param("itineraryId") Long itineraryId);
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE bookings
        SET status = 'CANCELLED'
        WHERE itinerary_id = :itineraryId
        AND status = 'PENDING'
        """, nativeQuery = true)
    void cancelPendingBookings(@Param("itineraryId") Long itineraryId);


    @Query(value = """
        SELECT COUNT(*) FROM destinations
        WHERE id = :destinationId
        AND status = 'ACTIVE'
        """, nativeQuery = true)
    Integer checkDestinationActive(@Param("destinationId") Long destinationId);

    @Query(value = """
        SELECT COUNT(*) FROM destinations
        WHERE id = :destinationId
        """, nativeQuery = true)
    Integer checkDestinationExists(@Param("destinationId") Long destinationId);


    @Query(value = """
        SELECT COALESCE(MAX(day_order), 0)
        FROM itinerary_days
        WHERE itinerary_id = :itineraryId
        """, nativeQuery = true)
    Integer getMaxDayOrder(@Param("itineraryId") Long itineraryId);


    @Query(value = """
        SELECT * FROM itineraries
        WHERE (:status IS NULL OR status = :status)
        AND start_date >= :startDate
        AND start_date <= :endDate
        ORDER BY created_at DESC
        """, nativeQuery = true)
    List<Itinerary> searchByStatusAndDateRange(
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}