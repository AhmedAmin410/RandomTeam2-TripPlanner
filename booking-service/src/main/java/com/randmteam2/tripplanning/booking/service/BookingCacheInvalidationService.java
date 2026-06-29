package com.randmteam2.tripplanning.booking.service;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookingCacheInvalidationService {

    private static final String CACHE_PREFIX = "booking-service::";

    @Nullable
    private final RedisTemplate<String, Object> redisTemplate;

    public BookingCacheInvalidationService(@Nullable RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Evict all booking-related read caches on any write.
     * Includes S5-F10 and S5-F11 because any booking status change
     * affects revenue analytics and payment history (MS3 §7 cache rules).
     */
    public void evictBookingReadCaches() {
        evictPatterns(
                cacheKey("booking::*"),
                cacheKey("S5-F1::*"),
                cacheKey("S5-F3::*"),
                cacheKey("S5-F6::*"),
                cacheKey("S5-F8::*"),
                cacheKey("S5-F9::*"),
                cacheKey("S5-F10::*"),
                cacheKey("S5-F11::*"),
                cacheKey("S5-SYNC-CONFIRMED-SUMMARY::*"),
                cacheKey("S5-SYNC-USER-TOTAL::*"),
                cacheKey("S5-SYNC-AGGREGATE::*")
        );
    }

    /**
     * Evict caches specifically for refund-related operations (S5-F2, S5-F12).
     * Also used by itinerary.cancelled consumer.
     */
    public void evictRefundRelatedCaches() {
        evictPatterns(
                cacheKey("booking::*"),
                cacheKey("S5-F1::*"),
                cacheKey("S5-F3::*"),
                cacheKey("S5-F6::*"),
                cacheKey("S5-F8::*"),
                cacheKey("S5-F9::*"),
                cacheKey("S5-F10::*"),
                cacheKey("S5-F11::*"),
                cacheKey("S5-SYNC-CONFIRMED-SUMMARY::*"),
                cacheKey("S5-SYNC-USER-TOTAL::*"),
                cacheKey("S5-SYNC-AGGREGATE::*")
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
        if (redisTemplate == null) {
            return;
        }
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