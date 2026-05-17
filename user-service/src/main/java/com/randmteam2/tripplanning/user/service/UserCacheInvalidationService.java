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

    // Invalidates S1-F3 (trip summary for specific user) and all S1-F9 (travel-style) caches
    public void evictItineraryCaches(Long userId) {
        evictPatterns(
                cacheKey("S1-F3::" + userId + "*"),
                cacheKey("S1-F9::*")
        );
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
                ScanOptions.scanOptions().match(pattern).count(1000).build())) {
            List<String> keys = new ArrayList<>();
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            System.err.println("[WARN] Redis cache invalidation failed for " + pattern + ": " + e.getMessage());
        }
    }
}
