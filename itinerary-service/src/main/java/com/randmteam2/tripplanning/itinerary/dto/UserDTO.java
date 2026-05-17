package com.randmteam2.tripplanning.itinerary.dto;

public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String status; // "ACTIVE", "DEACTIVATED"

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}