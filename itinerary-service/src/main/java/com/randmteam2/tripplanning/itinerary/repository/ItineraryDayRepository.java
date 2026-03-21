package com.randmteam2.tripplanning.itinerary.repository;

import com.randmteam2.tripplanning.itinerary.model.ItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long> {
    List<ItineraryDay> findByItineraryId(Long itineraryId);

    @Query(value = "SELECT * FROM itinerary_days WHERE itinerary_id = :itineraryId ORDER BY day_order ASC", nativeQuery = true)
    List<ItineraryDay> findByItineraryIdOrderByDayOrder(@Param("itineraryId") Long itineraryId);
}