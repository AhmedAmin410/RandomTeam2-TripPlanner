package com.randmteam2.tripplanning.user.dto;

import java.util.Map;

public record SavedDestinationDTO(
        String label,
        String destinationName,
        String country,
        Double latitude,
        Double longitude,
        Boolean isDefault,
        Map<String, Object> metadata
) {}