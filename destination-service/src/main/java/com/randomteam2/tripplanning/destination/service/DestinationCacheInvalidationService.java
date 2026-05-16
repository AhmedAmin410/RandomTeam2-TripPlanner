package com.randomteam2.tripplanning.destination.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DestinationCacheInvalidationService {

    private static final Logger logger = LoggerFactory.getLogger(DestinationCacheInvalidationService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public DestinationCacheInvalidationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void evictKey(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception exception) {
            logger.warn("Failed to evict Redis key: {}", key, exception);
        }
    }

    public void evictPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception exception) {
            logger.warn("Failed to evict Redis pattern: {}", pattern, exception);
        }
    }

    public void evictDestinationCaches(Long destinationId) {
        if (destinationId != null) {
            evictKey("destination-service::destination::" + destinationId);
            evictKey("destination-service::S2-F12::" + destinationId);
        }

        evictPattern("destination-service::S2-F1::*");
        evictPattern("destination-service::S2-F3::*");
        evictPattern("destination-service::S2-F5::*");
        evictPattern("destination-service::S2-F6::*");
        evictPattern("destination-service::S2-F9::*");
        evictPattern("destination-service::S2-F10::*");
    }

    public void evictDestinationReviewCaches(Long destinationId, Long reviewId) {
        if (reviewId != null) {
            evictKey("destination-service::destination-review::" + reviewId);
        }

        if (destinationId != null) {
            evictKey("destination-service::S2-F12::" + destinationId);
        }

        evictPattern("destination-service::S2-F9::*");
    }

    public void evictFullTextSearchCaches() {
        evictPattern("destination-service::S2-F10::*");
    }

    public void invalidateItinerarySagaCaches(Long destinationId) {
        if (destinationId == null) {
            return;
        }
        evictPattern("destination-service::S2-F3::" + destinationId + "::*");
        evictKey("destination-service::S2-F12::" + destinationId);
    }
}