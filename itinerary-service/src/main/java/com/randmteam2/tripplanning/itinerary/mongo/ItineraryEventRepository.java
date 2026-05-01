package com.randmteam2.tripplanning.itinerary.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryEventRepository extends MongoRepository<ItineraryEvent, String> {
    List<ItineraryEvent> findByItineraryIdOrderByTimestampDesc(Long itineraryId);
}