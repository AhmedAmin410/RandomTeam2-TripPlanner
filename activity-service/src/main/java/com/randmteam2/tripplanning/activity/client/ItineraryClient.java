package com.randmteam2.tripplanning.activity.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class ItineraryClient {

    private final RestTemplate restTemplate;
    private final String itineraryServiceUrl;

    public ItineraryClient(RestTemplate restTemplate,
                           @Value("${itinerary.service.url}") String itineraryServiceUrl) {
        this.restTemplate = restTemplate;
        this.itineraryServiceUrl = itineraryServiceUrl;
    }

    public void validateItineraryExists(Long itineraryId) {
        try {
            restTemplate.getForEntity(
                itineraryServiceUrl + "/api/itineraries/" + itineraryId,
                Object.class
            );
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Itinerary not found with id: " + itineraryId);
        }
    }
}