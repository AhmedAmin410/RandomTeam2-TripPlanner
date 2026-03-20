package com.randmteam2.tripplanning.itinerary.repository;

import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
}