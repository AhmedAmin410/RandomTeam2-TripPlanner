package com.randmteam2.tripplanning.booking.service;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookingCacheInvalidationService {

    private static final String CACHE_PREFIX = "booking-service::";

    private final RedisTemplate<String, Object> redisTemplate;

    public BookingCacheInvalidationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void evictBookingReadCaches() {
        evictPatterns(
                cacheKey("booking::*"),
                cacheKey("S5-F1::*"),
                cacheKey("S5-F3::*"),
                cacheKey("S5-F6::*"),
                cacheKey("S5-F8::*"),
                cacheKey("S5-F9::*")
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
