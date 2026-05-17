package com.randmteam2.tripplanning.user.service;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserCacheInvalidationService {

    private static final String CACHE_PREFIX = "user-service::";

    private final RedisTemplate<String, Object> redisTemplate;

    public UserCacheInvalidationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void evictUserReadCaches() {
        evictPatterns(
                cacheKey("user::*"),
                cacheKey("saved-destination::*"),
                cacheKey("S1-F1::*"),
                cacheKey("S1-F3::*"),
                cacheKey("S1-F5::*"),
                cacheKey("S1-F6::*"),
                cacheKey("S1-F8::*"),
                cacheKey("S1-F9::*"),
                cacheKey("S1-F12::*")
        );
    }

    /*
     * Requirement 2 / S1-EVENTS:
     * This method is used when itinerary-service publishes:
     * - itinerary.completed
     * - itinerary.cancelled
     *
     * It invalidates the cached trip summary for one specific user.
     *
     * Existing cache key in UserService:
     * user-service::S1-F3::{userId}
     */
    public void evictUserTripSummaryCache(Long userId) {
        if (userId == null) {
            return;
        }

        evictPatterns(
                cacheKey("S1-F3::" + userId + "*")
        );
    }

    /*
     * Requirement 2 / S1-EVENTS:
     * This method allows RabbitMQ cache invalidation events like:
     * invalidate.user-service.S1-F3.{userId}.all
     *
     * The event message can include a Redis key pattern, and this service
     * will delete all Redis keys matching that pattern.
     */
    public void evictByPattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return;
        }

        scanAndDelete(pattern);
    }

    private String cacheKey(String keyPattern) {
        return CACHE_PREFIX + keyPattern;
    }

    private void evictPatterns(String... patterns) {
        for (String pattern : patterns) {
            scanAndDelete(pattern);
        }
    }

    private void scanAndDelete(String pattern) {
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(pattern)
                        .count(1000)
                        .build())) {

            List<String> keys = new ArrayList<>();

            cursor.forEachRemaining(keys::add);

            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);

                System.out.println("[Redis] Deleted cache keys matching pattern: "
                        + pattern + " | count=" + keys.size());
            } else {
                System.out.println("[Redis] No cache keys found for pattern: " + pattern);
            }

        } catch (Exception e) {
            System.err.println("[WARN] Redis cache invalidation failed for "
                    + pattern + ": " + e.getMessage());
        }
    }
}