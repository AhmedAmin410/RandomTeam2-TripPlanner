package com.randmteam2.tripplanning.activity.controller;



import com.randmteam2.tripplanning.activity.dto.ActivitySummaryDTO;
import com.randmteam2.tripplanning.activity.dto.NearbyActivityDTO;
import com.randmteam2.tripplanning.activity.dto.BatchActivityRequest;
import com.randmteam2.tripplanning.activity.model.Activity;
import com.randmteam2.tripplanning.activity.service.ActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }


    @PostMapping
    public ResponseEntity<Activity> create(@RequestBody Activity activity) {
        return ResponseEntity.status(201).body(activityService.create(activity));
    }


    // --- S4-F3: Find Nearby Activities DTO ---
    @GetMapping("/nearby")
    public List<NearbyActivityDTO> getNearby(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam Double radiusKm) {
        return activityService.getNearbyActivities(lat, lon, radiusKm);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Activity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.getById(id));
    }
//getallgit add .
    @GetMapping
    public ResponseEntity<List<Activity>> getAll() {
        return ResponseEntity.ok(activityService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Activity> update(@PathVariable Long id,
                                           @RequestBody Activity activity) {
        return ResponseEntity.ok(activityService.update(id, activity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/itinerary/{itineraryId}")
    public ResponseEntity<Activity> createForItinerary(@PathVariable Long itineraryId,
                                                       @RequestBody Activity activity) {
        return ResponseEntity.status(201).body(activityService.createForItinerary(itineraryId, activity));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Integer>> createBatch(@RequestBody BatchActivityRequest request) {
        List<Activity> saved = activityService.createBatch(request.getItineraryId(), request.getActivities());
        return ResponseEntity.status(201).body(Map.of("count", saved.size()));
    }

    @GetMapping("/itinerary/{id}/latest")
    public ResponseEntity<Activity> getLatestByItinerary(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.getLatestByItineraryId(id));
    }

    @GetMapping("/metadata/search")
    public ResponseEntity<List<Activity>> searchByMetadata(@RequestParam String key,
                                                           @RequestParam String operator,
                                                           @RequestParam String value) {
        return ResponseEntity.ok(activityService.findByMetadata(key, operator, value));
    }

    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, Integer>> purge(@RequestParam int olderThanDays) {
        int deletedCount = activityService.purgeOlderThan(olderThanDays);
        return ResponseEntity.ok(Map.of("deletedCount", deletedCount));
    }

    // F6: History
// F6: History update to LocalDate
    @GetMapping("/history")
    public List<Activity> getHistory(
            @RequestParam("startDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate start,
            @RequestParam("endDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate end,
            @RequestParam(value = "category", required = false) String category) {
        return activityService.getActivitiesByHistory(start, end, category);
    }

    // S4-F8: Summary update to LocalDate
    @GetMapping("/itinerary/{id}/summary")
    public ResponseEntity<ActivitySummaryDTO> getItinerarySummary(
            @PathVariable Long id,
            @RequestParam("startDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate start,
            @RequestParam("endDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate end) {
        return ResponseEntity.ok(activityService.getActivitySummary(id, start, end));
    }

}