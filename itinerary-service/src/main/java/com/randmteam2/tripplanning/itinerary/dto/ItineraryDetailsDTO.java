package com.randmteam2.tripplanning.itinerary.dto;

import com.randmteam2.tripplanning.itinerary.model.ItineraryDay;
import java.util.List;
import java.util.Map;

public record ItineraryDetailsDTO(
        Long itineraryId,
        Long userId,
        Long destinationId,
        String title,
        String status,
        Double estimatedBudget,
        Map<String, Object> metadata,
        List<ItineraryDay> days,
        int totalDays,
        long completedDays
) {}