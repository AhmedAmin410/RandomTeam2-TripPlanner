package com.randmteam2.tripplanning.itinerary.neo4j;// UserNodeRepository.java
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import com.randmteam2.tripplanning.itinerary.neo4j.RecommendationResult;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {
    Optional<UserNode> findByUserId(Long userId);

    @Query("""
        MATCH (target:User {userId: $userId})-[:VISITED]->(shared:Destination)
              <-[:VISITED]-(similar:User)
        WHERE similar.userId <> $userId
        MATCH (similar)-[:VISITED]->(recommended:Destination)
        WHERE NOT (target)-[:VISITED]->(recommended)
        RETURN recommended.destinationId AS destinationId,
               COUNT(DISTINCT similar) AS score
        ORDER BY score DESC
        LIMIT $limit
        """)
    List<RecommendationResult> findRecommendations(Long userId, int limit);
}