package com.randmteam2.tripplanning.activity.service;

import com.randmteam2.tripplanning.activity.dto.*;
import com.randmteam2.tripplanning.activity.model.Activity;
import com.randmteam2.tripplanning.activity.model.ActivityLifecycleEvent;
import com.randmteam2.tripplanning.activity.repository.ActivityLifecycleEventRepository;
import com.randmteam2.tripplanning.contracts.feign.ItineraryServiceClient;
import feign.FeignException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.randmteam2.tripplanning.activity.repository.ActivityRepository;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityLifecycleEventRepository lifecycleEventRepository;
    private final ItineraryServiceClient itineraryServiceClient;

    public ActivityService(ActivityRepository activityRepository,
                           ActivityLifecycleEventRepository lifecycleEventRepository,
                           ItineraryServiceClient itineraryServiceClient) {
        this.activityRepository = activityRepository;
        this.lifecycleEventRepository = lifecycleEventRepository;
        this.itineraryServiceClient = itineraryServiceClient;
    }

    private void validateItineraryExists(Long itineraryId) {
        try {
            itineraryServiceClient.getItinerary(itineraryId);
        } catch (FeignException.NotFound e) {
            throw new RuntimeException("Itinerary not found with id: " + itineraryId);
        } catch (FeignException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to validate itinerary " + itineraryId,
                    e);
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

                    return NearbyActivityDTO.builder()
                            .activityId(activity.getId())
                            .name(activity.getName())
                            .category(activity.getCategory().name())
                            .lat(activity.getLatitude())
                            .lon(activity.getLongitude())
                            .distanceKm(distance)
                            .build();
                }).filter(dto -> dto.distanceKm() <= radiusKm)
                .sorted(Comparator.comparing(NearbyActivityDTO::distanceKm))
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

        return ActivitySummaryDTO.builder()
                .itineraryId(itineraryId)
                .totalActivities(totalActivities)
                .averageCost(averageCost)
                .maxCost(maxCost)
                .firstScheduledTime(firstScheduledTime)
                .lastScheduledTime(lastScheduledTime)
                .build();
    }

    // --- S4-F9: Low-Cost Activities (Updated to new DTO spec) ---
    public List<BudgetActivityDTO> getBudgetFriendlyActivities(Double maxCost, Integer sinceMinutes) {
        List<Activity> activities = activityRepository.findBudgetFriendly(maxCost, sinceMinutes);

        return activities.stream().map(activity -> {
            Double costValue = 0.0;
            if (activity.getMetadata() != null && activity.getMetadata().containsKey("cost")) {
                try {
                    costValue = Double.valueOf(activity.getMetadata().get("cost").toString());
                } catch (NumberFormatException e) {
                    costValue = 0.0;
                }
            }

            return BudgetActivityDTO.builder()
                    .activityId(activity.getId())
                    .name(activity.getName())
                    .category(activity.getCategory().name())
                    .cost(costValue)
                    .latitude(activity.getLatitude())
                    .longitude(activity.getLongitude())
                    .scheduledTime(activity.getScheduledTime())
                    .build();
        }).collect(Collectors.toList());
    }

    // --- S4-F12: Activity Event Timeline ---
    public List<ActivityEventDTO> getActivityTimeline(Long activityId, Instant startTime, Instant endTime) {
        // Validate activity exists in PG (404 if not)
        activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + activityId));

        List<ActivityLifecycleEvent> events;

        if (startTime != null && endTime != null) {
            events = lifecycleEventRepository.findByActivityIdAndTimestampBetween(activityId, startTime, endTime);
        } else {
            events = lifecycleEventRepository.findByActivityId(activityId);
        }

        // Sort newest first (Cassandra clustering already does this, but ensure it)
        return events.stream()
                .sorted(Comparator.comparing(ActivityLifecycleEvent::getTimestamp).reversed())
                .map(event -> ActivityEventDTO.builder()
                        .eventId(event.getEventId())
                        .activityId(event.getActivityId())
                        .status(event.getStatus())
                        .timestamp(event.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }



}
