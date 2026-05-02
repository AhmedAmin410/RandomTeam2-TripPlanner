package com.randmteam2.tripplanning.itinerary.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DestinationNodeRepository extends Neo4jRepository<DestinationNode, Long> {
    Optional<DestinationNode> findByDestinationId(Long destinationId);
}