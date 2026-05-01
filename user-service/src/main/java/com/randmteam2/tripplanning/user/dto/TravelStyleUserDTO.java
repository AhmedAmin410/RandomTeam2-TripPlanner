package com.randmteam2.tripplanning.user.dto;

import java.util.Map;

public class TravelStyleUserDTO {

    private Long userId;
    private String name;
    private String email;
    private String role;
    private Map<String, Object> preferences;

    private TravelStyleUserDTO() {}

    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Map<String, Object> getPreferences() { return preferences; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long userId;
        private String name;
        private String email;
        private String role;
        private Map<String, Object> preferences;

        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder preferences(Map<String, Object> preferences) { this.preferences = preferences; return this; }

        public TravelStyleUserDTO build() {
            TravelStyleUserDTO dto = new TravelStyleUserDTO();
            dto.userId = this.userId;
            dto.name = this.name;
            dto.email = this.email;
            dto.role = this.role;
            dto.preferences = this.preferences;
            return dto;
        }
    }
}
