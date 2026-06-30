package com.randmteam2.tripplanning.user.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    TRAVELER,
    ADMIN;

    @JsonCreator
    public static Role fromJson(String value) {
        if (value == null || value.isBlank()) {
            return TRAVELER;
        }
        String normalized = value.trim().toUpperCase();
        if ("USER".equals(normalized) || "CUSTOMER".equals(normalized)) {
            return TRAVELER;
        }
        return Role.valueOf(normalized);
    }
}
