package com.randmteam2.tripplanning.activity.service;

import com.randmteam2.tripplanning.activity.dto.ActivitySummaryDTO;
import com.randmteam2.tripplanning.activity.dto.NearbyActivityDTO;
import com.randmteam2.tripplanning.activity.model.Activity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.randmteam2.tripplanning.activity.repository.ActivityRepository;

import java.time.LocalDate;
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

    public Activity createForItinerary(Long itineraryId, Activity activity) {
        validateItineraryExists(itineraryId);
        activity.setItineraryId(itineraryId);
        return activityRepository.save(activity);
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
    public List<Activity> createBatch(Long itineraryId, List<Activity> activities) {
        validateItineraryExists(itineraryId);
        for (Activity activity : activities) {
            if (activity.getLatitude() == null || activity.getLatitude() < -90 || activity.getLatitude() > 90) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90");
            }
            if (activity.getLongitude() == null || activity.getLongitude() < -180 || activity.getLongitude() > 180) {
                throw new IllegalArgumentException("Longitude must be between -180 and 180");
            }
            activity.setItineraryId(itineraryId);
        }
        return activityRepository.saveAll(activities);
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

    // --- S4-F6: Activities in Date Range (Using LocalDate) ---
    public List<Activity> getActivitiesByHistory(LocalDate start, LocalDate end, String category) {
        String categoryParam = (category != null && !category.isEmpty()) ? category : null;
        return activityRepository.findByHistory(start, end, categoryParam);
    }

    // --- S4-F8: Activity Summary DTO (Fixed Type Conversion) ---
    public ActivitySummaryDTO getActivitySummary(Long itineraryId, LocalDate start, LocalDate end) {
        if (!activityRepository.existsByItineraryId(itineraryId)) {
            throw new RuntimeException("Itinerary not found with id: " + itineraryId);
        }

        // 1. Convert LocalDate boundaries to LocalDateTime for the query
        LocalDateTime startDT = start.atStartOfDay();
        LocalDateTime endDT = end.atTime(23, 59, 59);

        // 2. Execute query
        Object result = activityRepository.getActivitySummaryRaw(itineraryId, startDT, endDT);
        Object[] row = (Object[]) result;

        // 3. Extract and Cast values correctly
        Long totalActivities = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        Double averageCost = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        Double maxCost = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

        // 4. FIX: Cast to Timestamp THEN convert to LocalDateTime
        LocalDateTime firstScheduledTime = row[3] != null ? ((java.sql.Timestamp) row[3]).toLocalDateTime() : null;
        LocalDateTime lastScheduledTime = row[4] != null ? ((java.sql.Timestamp) row[4]).toLocalDateTime() : null;

        // 5. Return with all 6 arguments
        return new ActivitySummaryDTO(
                itineraryId,
                totalActivities,
                averageCost,
                maxCost,
                firstScheduledTime,
                lastScheduledTime
        );
    }

}