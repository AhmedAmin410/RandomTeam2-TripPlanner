package com.randmteam2.tripplanning.itinerary.dto;

import java.time.LocalDate;
import java.util.Map;

public record ItineraryDayRequest(
        LocalDate date,
        String title,
        String description,
        Map<String, Object> metadata
) {}
