package com.randmteam2.tripplanning.activity.service;

import com.randmteam2.tripplanning.activity.dto.ActivityEventDTO;
import com.randmteam2.tripplanning.activity.dto.ActivitySummaryDTO;
import com.randmteam2.tripplanning.activity.dto.BudgetActivityDTO;
import com.randmteam2.tripplanning.activity.dto.NearbyActivityDTO;
import com.randmteam2.tripplanning.activity.model.Activity;
import com.randmteam2.tripplanning.activity.model.ActivityLifecycleEvent;
import com.randmteam2.tripplanning.activity.repository.ActivityLifecycleEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.randmteam2.tripplanning.activity.repository.ActivityRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityLifecycleEventRepository lifecycleEventRepository;


    private String itineraryServiceUrl;

    public ActivityService(ActivityRepository activityRepository,
                           ActivityLifecycleEventRepository lifecycleEventRepository) {
        this.activityRepository = activityRepository;
        this.lifecycleEventRepository = lifecycleEventRepository;
    }
    private void validateItineraryExists(Long itineraryId) {
        Integer count = activityRepository.checkItineraryExists(itineraryId);
        if (count == null || count == 0) {
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



    // â”€â”€â”€ S4-F10: Activity Analytics Dashboard â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public ActivityAnalyticsDTO getAnalyticsDashboard(String startDate, String endDate) {
        notifyObservers("ANALYTICS_VIEWED", Map.of(
                "action", "ANALYTICS_VIEWED",
                "startDate", startDate,
                "endDate", endDate
        ));
        return getAnalyticsDashboardCached(startDate, endDate);
    }

    @Cacheable(value = "activity-service::S4-F10", key = "#startDate + ':' + #endDate")
    public ActivityAnalyticsDTO getAnalyticsDashboardCached(String startDate, String endDate) {
        List<Object[]> rows = activityRepository.getAnalyticsByCategory(startDate, endDate);

        long total = 0;
        double totalCost = 0;
        double totalDuration = 0;
        Map<String, Long> byCategory = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String category = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double avgCost = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            double avgDuration = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            total += count;
            totalCost += avgCost * count;
            totalDuration += avgDuration * count;
            byCategory.put(category, count);
        }

        return ActivityAnalyticsDTO.builder()
                .totalActivities(total)
                .averageCost(total > 0 ? totalCost / total : 0.0)
                .averageDurationHours(total > 0 ? totalDuration / total : 0.0)
                .activitiesByCategory(byCategory)
                .build();
    }


    @Transactional
    public long createBatch(Long itineraryId, List<Activity> activities) {
        if (activityRepository.countItinerary(itineraryId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }
        for (Activity a : activities) {
            a.setItineraryId(itineraryId);
            if (a.getLatitude() == null  a.getLatitude() < -90
            a.getLatitude() > 90) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Latitude must be between -90 and 90");
            }
            if (a.getLongitude() == null  a.getLongitude() < -180
            a.getLongitude() > 180) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Longitude must be between -180 and 180");
            }
        }
        long saved = activityRepository.saveAll(activities).size();
        notifyObservers("ACTIVITY", Map.of("itineraryId", itineraryId, "action", "BATCH_CREATED", "details", "Batch of " + saved + " activities created for itinerary: " + itineraryId));
        return saved;
    }



    @Transactional
    public long purge(int olderThanDays) {
        String cutoff = LocalDateTime.now().minusDays(olderThanDays)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        long count = activityRepository.countOlderThan(cutoff);
        activityRepository.deleteOlderThan(cutoff);
        notifyObservers("ACTIVITY", Map.of("action", "OLD_DATA_PURGED", "details", "Purged " + count + " activities older than " + olderThanDays + " days"));
        return count;
    }

    // â”€â”€â”€ S4-F8: Activity Summary â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Cacheable(value = "activity-service::S4-F8", key = "#itineraryId + ':' + #startDate + ':' + #endDate")
    public ActivitySummaryDTO getSummary(Long itineraryId, String startDate, String endDate) {
        if (activityRepository.countItinerary(itineraryId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }
        Object[] row = activityRepository.getActivitySummary(itineraryId, startDate, endDate);
        ActivitySummaryDTO dto = new ActivitySummaryDTO();
        dto.setItineraryId(itineraryId);
        dto.setTotalActivities(row[0] != null ? ((Number) row[0]).longValue() : 0L);
        dto.setAverageCost(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
        dto.setMaxCost(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0);
        dto.setFirstScheduledTime(row[3] != null ? row[3].toString() : null);
        dto.setLastScheduledTime(row[4] != null ? row[4].toString() : null);
        return dto;
    }

    // â”€â”€â”€ S4-F9: Budget-Friendly Activities â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Cacheable(value = "activity-service::S4-F9", key = "#maxCost + ':' + #sinceMinutes")
    public List<BudgetActivityDTO> getBudgetFriendly(double maxCost, int sinceMinutes) {
        String since = LocalDateTime.now().minusMinutes(sinceMinutes)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<Activity> activities = activityRepository.findBudgetFriendly(maxCost, since);
        return activities.stream().map(a -> {
            BudgetActivityDTO dto = new BudgetActivityDTO();
            dto.setActivityId(a.getId());
            dto.setName(a.getName());
            dto.setCategory(a.getCategory() != null ? a.getCategory().name() : null);
            dto.setLatitude(a.getLatitude());
            dto.setLongitude(a.getLongitude());
            dto.setCost(a.getMetadata() != null && a.getMetadata().get("cost") != null
                    ? ((Number) a.getMetadata().get("cost")).doubleValue() : null);
            dto.setScheduledTime(a.getScheduledTime() != null ? a.getScheduledTime().toString() : null);
            return dto;
        }).collect(Collectors.toList());
    }



    public void recordLifecycleEvent(Long activityId, String status, String notes) {
        Activity activity = getActivityById(activityId);

        Set<String> validStatuses = Set.of("BOOKED", "STARTED", "COMPLETED", "CANCELLED");
        if (!validStatuses.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }

        ActivityLifecycleEvent event = new ActivityLifecycleEvent();
        event.setActivityId(activityId);
        event.setTimestamp(Instant.now());
        event.setStatus(status);
        event.setCategory(activity.getCategory() != null ? activity.getCategory().name() : null);
        event.setLatitude(activity.getLatitude());
        event.setLongitude(activity.getLongitude());
        event.setNotes(notes);

        try {
            lifecycleEventRepository.save(event);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cassandra write failed: " + e.getMessage());
        }

        notifyObservers("EVENT_RECORDED", Map.of(
                "activityId", activityId,
                "status", status,
                "action", "EVENT_RECORDED"
        ));

        cacheUtils.evictByPattern("activity-service::S4-F12::" + activityId + "*");
        cacheUtils.evictByPattern("activity-service::S4-F10::*");
    }

    // â”€â”€â”€ S4-F12: Activity Event Timeline â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Cacheable(value = "activity-service::S4-F12", key = "#activityId + ':' + #startTime + ':' + #endTime")
    public List<ActivityEventDTO> getEventTimeline(Long activityId, String startTime, String endTime) {
        getActivityById(activityId); // 404 check

        List<ActivityLifecycleEvent> events;
        try {
            if (startTime != null && endTime != null) {
                Instant start = Instant.parse(startTime);
                Instant end = Instant.parse(endTime);
                events = lifecycleEventRepository.findByActivityIdAndTimestampBetween(activityId, start, end);
            } else {
                events = lifecycleEventRepository.findByActivityId(activityId);
            }
        } catch (Exception e) {
            return List.of();
        }

        return events.stream().map(e -> {
            ActivityEventDTO dto = new ActivityEventDTO();
            dto.setTimestamp(e.getTimestamp());
            dto.setStatus(e.getStatus());
            dto.setCategory(e.getCategory());
            dto.setLatitude(e.getLatitude());
            dto.setLongitude(e.getLongitude());
            dto.setNotes(e.getNotes());
            return dto;
        }).collect(Collectors.toList());
    }




    @Cacheable(value = "activity-service::S4-F10", key = "#startDate + ':' + #endDate")
    public ActivityAnalyticsDTO getAnalyticsDashboardCached(String startDate, String endDate) {
        List<Object[]> rows = activityRepository.getAnalyticsByCategory(startDate, endDate);

        long total = 0;
        double totalCost = 0;
        double totalDuration = 0;
        Map<String, Long> byCategory = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String category = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double avgCost = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            double avgDuration = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            total += count;
            totalCost += avgCost * count;
            totalDuration += avgDuration * count;
            byCategory.put(category, count);
        }




}