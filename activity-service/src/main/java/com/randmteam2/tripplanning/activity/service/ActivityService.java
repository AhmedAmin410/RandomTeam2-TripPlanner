package com.randmteam2.tripplanning.activity.service;

import com.randmteam2.tripplanning.activity.dto.NearbyActivityDTO;
import com.randmteam2.tripplanning.activity.model.Activity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.randmteam2.tripplanning.activity.repository.ActivityRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final RestTemplate restTemplate;

    @Value("${itinerary.service.url:http://localhost:8083}")
    private String itineraryServiceUrl;

    public ActivityService(ActivityRepository activityRepository, RestTemplate restTemplate) {
        this.activityRepository = activityRepository;
        this.restTemplate = restTemplate;
    }

    private void validateItineraryExists(Long itineraryId) {
        try {
            restTemplate.getForEntity(itineraryServiceUrl + "/api/itineraries/" + itineraryId, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Itinerary not found with id: " + itineraryId);
        }
    }


    public Activity create(Activity activity) {
        return activityRepository.save(activity);
    }

    public Activity getById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + id));
    }

    public List<Activity> getAll() {
        return activityRepository.findAll();
    }

    public Activity update(Long id, Activity updated) {
        Activity existing = getById(id);
        existing.setItineraryId(updated.getItineraryId());
        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setLatitude(updated.getLatitude());
        existing.setLongitude(updated.getLongitude());
        existing.setScheduledTime(updated.getScheduledTime());
        existing.setMetadata(updated.getMetadata());
        return activityRepository.save(existing);
    }

    public void delete(Long id) {
        getById(id);
        activityRepository.deleteById(id);
    }

    public List<Activity> findByMetadata(String key, String operator, String value) {
        if (key == null || key.isBlank() || operator == null || operator.isBlank() || value == null || value.isBlank()) {
            throw new IllegalArgumentException("key, operator, and value must not be blank");
        }

        return switch (operator) {
            case "eq" -> activityRepository.findByMetadataEq(key, value);
            case "gt" -> activityRepository.findByMetadataGt(key, value);
            case "lt" -> activityRepository.findByMetadataLt(key, value);
            default -> throw new IllegalArgumentException("operator must be one of [eq, gt, lt]");
        };
    }

    public Activity getLatestByItineraryId(Long itineraryId) {
        validateItineraryExists(itineraryId);
        Activity latest = activityRepository.findLatestByItineraryId(itineraryId);
        if (latest == null) {
            throw new RuntimeException("No activities not found for itinerary: " + itineraryId);
        }
        return latest;
    }

    @Transactional
    public int purgeOlderThan(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        return activityRepository.deleteOlderThan(cutoff);
    }

    // --- S4-F3: Find Nearby Activities DTO ---
    public List<NearbyActivityDTO> getNearbyActivities(Double userLat, Double userLon, Double radiusKm) {
        List<Activity> allActivities = activityRepository.findAll();

        return allActivities.stream()
                .map(activity -> {
                    double latDiff = activity.getLatitude() - userLat;
                    double lonDiff = activity.getLongitude() - userLon;
                    double distance = Math.sqrt(Math.pow(latDiff, 2) + Math.pow(lonDiff, 2)) * 111;

                    return new NearbyActivityDTO(
                            activity.getId(),
                            activity.getName(),
                            activity.getCategory().name(), // Added .name() to fix Enum-to-String error
                            activity.getLatitude(),
                            activity.getLongitude(),
                            distance
                    );
                })
                .filter(dto -> dto.getDistanceKm() <= radiusKm)
                .sorted(Comparator.comparing(NearbyActivityDTO::getDistanceKm))
                .collect(Collectors.toList());
    }
}