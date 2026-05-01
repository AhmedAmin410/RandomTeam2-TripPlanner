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
        AND CAST(status AS VARCHAR) = 'ACTIVE'
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

    @Query(value = """
        SELECT COUNT(*) FROM itineraries
        WHERE destination_id = :destinationId
        AND status IN ('DRAFT', 'PLANNED', 'IN_PROGRESS')
        """, nativeQuery = true)
    Integer countActiveItinerariesForDestination(@Param("destinationId") Long destinationId);

    @Query(value = """
        SELECT * FROM itineraries
        WHERE metadata ->> :key = :value
        """, nativeQuery = true)
    List<Itinerary> filterByMetadata(
            @Param("key") String key,
            @Param("value") String value
    );

    @Query(value = """
    SELECT COUNT(*) as total,
        SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
        SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled,
        COALESCE(SUM(estimated_budget), 0) as totalBudget,
        COALESCE(AVG(CASE WHEN status = 'COMPLETED' THEN estimated_budget END), 0) as avgBudget
    FROM itineraries
    WHERE start_date >= :startDate
    AND start_date <= :endDate
    """, nativeQuery = true)
    Object[] getAnalytics(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    boolean existsByUserEmail(String email);

    @Query(value = """
    SELECT
        COUNT(*) as total,
        COALESCE(SUM(estimated_budget), 0) as totalBudget,
        COALESCE(AVG(estimated_budget), 0) as avgBudget,
        SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
        SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled,
        SUM(CASE WHEN status = 'PLANNED' THEN 1 ELSE 0 END) as planned,
        SUM(CASE WHEN status = 'DRAFT' THEN 1 ELSE 0 END) as draft,
        SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgress
    FROM itineraries
    WHERE start_date >= :startDate
    AND start_date <= :endDate
    """, nativeQuery = true)
    Object[] getDashboardAnalytics(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}