package com.randomteam2.tripplanning.destination.dto;

public class DestinationSearchResultDTO {

    private Long id;
    private String name;
    private String country;
    private String category;
    private String description;
    private String highlights;
    private Double rating;
    private Integer totalRatings;
    private String status;

    public DestinationSearchResultDTO() {}

    public DestinationSearchResultDTO(Long id, String name, String country, String category,
                                      String description, String highlights, Double rating,
                                      Integer totalRatings, String status) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.category = category;
        this.description = description;
        this.highlights = highlights;
        this.rating = rating;
        this.totalRatings = totalRatings;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getHighlights() { return highlights; }
    public void setHighlights(String highlights) { this.highlights = highlights; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getTotalRatings() { return totalRatings; }
    public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
