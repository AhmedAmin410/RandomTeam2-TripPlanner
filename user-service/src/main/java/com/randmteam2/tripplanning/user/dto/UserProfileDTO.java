package com.randmteam2.tripplanning.user.dto;

import java.util.List;
import java.util.Map;

public record UserProfileDTO(
        Long userId,
        String name,
        String email,
        String phone,
        Map<String, Object> preferences,
        List<SavedDestinationDTO> savedDestinations,
        int totalSavedDestinations
) {}