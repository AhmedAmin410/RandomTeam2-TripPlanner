package com.randomteam2.tripplanning.destination.dto;

import java.util.Map;

/**
 * DTO returned by GET /api/destinations/{id}.
 * Contains exactly the fields that S3 and S5 Feign callers expect:
 * id, name, country, category, status, rating, totalRatings, details.
 */
public record DestinationDTO(
        Long id,
        String name,
        String country,
        String category,
        String status,
        Double rating,
        Integer totalRatings,
        Map<String, Object> details
) {}
