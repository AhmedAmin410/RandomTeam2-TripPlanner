package com.randmteam2.tripplanning.activity.service;

import com.randmteam2.tripplanning.activity.model.Activity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.randmteam2.tripplanning.activity.repository.ActivityRepository;
import com.randmteam2.tripplanning.activity.client.ItineraryClient;
import com.randmteam2.tripplanning.activity.dto.ActivityRequest;
import com.randmteam2.tripplanning.activity.dto.BatchActivityRequest;
import com.randmteam2.tripplanning.activity.dto.BatchActivityResponse;
import java.util.ArrayList;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;

    private final ItineraryClient itineraryClient;

    public ActivityService(ActivityRepository activityRepository, ItineraryClient itineraryClient) {
        this.activityRepository = activityRepository;
        this.itineraryClient = itineraryClient;
    }

    public Activity getLatestByItineraryId(Long itineraryId) {
        itineraryClient.validateItineraryExists(itineraryId);
        return activityRepository.findLatestByItineraryId(itineraryId)
            .orElseThrow(() -> new RuntimeException("No activities found for itinerary with id: " + itineraryId));
    }

    public Activity createForItinerary(Long itineraryId, ActivityRequest request) {
        itineraryClient.validateItineraryExists(itineraryId);
        Activity activity = new Activity();
        activity.setItineraryId(itineraryId);
        activity.setName(request.getName());
        activity.setCategory(request.getCategory());
        activity.setLatitude(request.getLat());
        activity.setLongitude(request.getLon());
        activity.setScheduledTime(request.getScheduledTime());
        activity.setMetadata(request.getMetadata());
        return activityRepository.save(activity);
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

    @Transactional
    public int purgeOlderThan(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        return activityRepository.deleteOlderThan(cutoff);
    }

    @Transactional
    public BatchActivityResponse createBatch(BatchActivityRequest request) {
        itineraryClient.validateItineraryExists(request.getItineraryId());

        var activities = new ArrayList<Activity>();
        for (ActivityRequest req : request.getActivities()) {
            if (req.getLat() == null || req.getLat() < -90 || req.getLat() > 90) {
                throw new IllegalArgumentException(
                    "lat must be in [-90, 90], got: " + req.getLat());
            }
            if (req.getLon() == null || req.getLon() < -180 || req.getLon() > 180) {
             throw new IllegalArgumentException(
                    "lon must be in [-180, 180], got: " + req.getLon());
         }
            Activity activity = new Activity();
            activity.setItineraryId(request.getItineraryId());
            activity.setName(req.getName());
            activity.setCategory(req.getCategory());
            activity.setLatitude(req.getLat());
            activity.setLongitude(req.getLon());
            activity.setScheduledTime(req.getScheduledTime());
            activity.setMetadata(req.getMetadata());
            activities.add(activity);
        }
        activityRepository.saveAll(activities);
        return new BatchActivityResponse(activities.size());
    }
}
}