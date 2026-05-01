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
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long userId;
        private String name;
        private String email;
        private String phone;
        private Map<String, Object> preferences;
        private List<SavedDestinationDTO> savedDestinations;
        private int totalSavedDestinations;

        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder preferences(Map<String, Object> preferences) { this.preferences = preferences; return this; }
        public Builder savedDestinations(List<SavedDestinationDTO> savedDestinations) { this.savedDestinations = savedDestinations; return this; }
        public Builder totalSavedDestinations(int totalSavedDestinations) { this.totalSavedDestinations = totalSavedDestinations; return this; }

        public UserProfileDTO build() {
            return new UserProfileDTO(userId, name, email, phone, preferences, savedDestinations, totalSavedDestinations);
        }
    }
}
