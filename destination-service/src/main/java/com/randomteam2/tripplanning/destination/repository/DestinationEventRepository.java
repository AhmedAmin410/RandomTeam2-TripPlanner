package com.randomteam2.tripplanning.destination.repository;

import com.randomteam2.tripplanning.destination.mongo.DestinationEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DestinationEventRepository extends MongoRepository<DestinationEvent, String> {

    List<DestinationEvent> findByDestinationIdOrderByTimestampDesc(Long destinationId);
}