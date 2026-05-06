// service/RecommendationService.java
package com.randmteam2.tripplanning.itinerary.service;

import com.randmteam2.tripplanning.itinerary.dto.DestinationRecommendationDTO;
import com.randmteam2.tripplanning.itinerary.neo4j.RecommendationResult;
import com.randmteam2.tripplanning.itinerary.neo4j.UserNodeRepository;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final UserNodeRepository userNodeRepository;
    private final ItineraryRepository itineraryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public RecommendationService(UserNodeRepository userNodeRepository,
                                 ItineraryRepository itineraryRepository,
                                 RedisTemplate<String, Object> redisTemplate) {
        this.userNodeRepository = userNodeRepository;
        this.itineraryRepository = itineraryRepository;
        this.redisTemplate = redisTemplate;
    }

    public List<DestinationRecommendationDTO> getRecommendations(Long userId, int limit) {
        String cacheKey = "itinerary-service::S3-F12::" + userId + "::" + limit;

        // Try cache first
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return (List<DestinationRecommendationDTO>) cached;
            }
        } catch (Exception e) {
            // soft dependency — Redis down, continue
        }

        // Neo4j graph traversal
        List<RecommendationResult> results;
        try {
            results = userNodeRepository.findRecommendations(userId, limit);
        } catch (Exception e) {
            return Collections.emptyList();
        }

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        // Enrich with PG destination details
        List<DestinationRecommendationDTO> recommendations = results.stream()
                .map(r -> {
                    Object[] dest = itineraryRepository.getDestinationById(r.getDestinationId());
                    if (dest == null) return null;
                    return DestinationRecommendationDTO.builder()
                            .destinationId(r.getDestinationId())
                            .name(dest.length > 1 && dest[1] != null ? dest[1].toString() : "")
                            .country(dest.length > 2 && dest[2] != null ? dest[2].toString() : "")
                            .category(dest.length > 3 && dest[3] != null ? dest[3].toString() : "")
                            .score(r.getScore())
                            .build();
                })
                .filter(d -> d != null)
                .collect(Collectors.toList());

        // Cache for 5 minutes
        try {
            redisTemplate.opsForValue().set(cacheKey, recommendations, Duration.ofMinutes(5));
        } catch (Exception e) {
            // soft dependency
        }

        return recommendations;
    }
}