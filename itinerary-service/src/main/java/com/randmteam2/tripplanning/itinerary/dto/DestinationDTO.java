package com.randmteam2.tripplanning.itinerary.dto;

public class DestinationDTO {
    private Long id;
    private String name;
    private String country;
    private String category;
    private String status; // "ACTIVE", "INACTIVE"

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}