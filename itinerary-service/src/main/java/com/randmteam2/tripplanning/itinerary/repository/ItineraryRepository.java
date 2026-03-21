package com.randmteam2.tripplanning.itinerary.repository;

import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    @Query(value = """
        SELECT COALESCE(SUM(b.amount), 0)
        FROM bookings b
        WHERE b.itinerary_id = :itineraryId
        AND b.status = 'CONFIRMED'
        """, nativeQuery = true)
    Double sumConfirmedBookings(@Param("itineraryId") Long itineraryId);
}