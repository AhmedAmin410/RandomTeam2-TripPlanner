package com.randmteam2.tripplanning.activity.controller;



import com.randmteam2.tripplanning.activity.dto.*;
import com.randmteam2.tripplanning.activity.model.Activity;
import com.randmteam2.tripplanning.activity.service.ActivityService;
import com.randmteam2.tripplanning.activity.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private static final Logger log = LoggerFactory.getLogger(ActivityController.class);

    private final ActivityService activityService;
    private final JwtService jwtService;

    public ActivityController(ActivityService activityService, JwtService jwtService) {
        this.activityService = activityService;
        this.jwtService = jwtService;
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
        log.info("Received POST /api/activities/itinerary/{}", itineraryId);
        Activity created = activityService.createForItinerary(itineraryId, activity);
        log.info("Returning 201 for POST /api/activities/itinerary/{}", itineraryId);
        return ResponseEntity.status(201).body(created);
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchCreate(@RequestBody BatchActivityRequest request) {
        log.info("Received POST /api/activities/batch for itinerary {}", request.getItineraryId());
        List<Activity> created = activityService.batchCreate(request.getItineraryId(), request.getActivities());
        log.info("Returning 201 for POST /api/activities/batch, count={}", created.size());
        return ResponseEntity.status(201).body(Map.of("count", created.size(), "activities", created));
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

    // AC-F9: Budget-Friendly
    @GetMapping("/budget-friendly")
    public List<BudgetActivityDTO> getBudgetActivities(
            @RequestParam Double maxCost,
            @RequestParam Integer sinceMinutes) {
        return activityService.getBudgetFriendlyActivities(maxCost, sinceMinutes);
    }

    // S4-F12: Activity Event Timeline
    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<ActivityEventDTO>> getActivityTimeline(
            @PathVariable Long id,
            @RequestParam(value = "startTime", required = false) Instant startTime,
            @RequestParam(value = "endTime", required = false) Instant endTime) {
        return ResponseEntity.ok(activityService.getActivityTimeline(id, startTime, endTime));
    }

    // S4-F10: Activity Analytics Dashboard
    @GetMapping("/analytics")
    public ResponseEntity<?> analyticsDashboard(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // The JWT filter has already validated the token (401 on missing/invalid),
        // so by this point the header is present and parseable.
        Long callerUid = null;
        String role = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                callerUid = jwtService.extractUserId(token);
                role = jwtService.extractRole(token);
            } catch (Exception ignored) {
                // fall through — treated as non-admin / no uid below
            }
        }
        boolean admin = "ADMIN".equals(role);

        if (userId == null && !admin) {
            ActivityAnalyticsDTO analytics = activityService.getAnalyticsDashboard(startDate, endDate);
            if (analytics.getTotalActivities() != null && analytics.getTotalActivities() > 0) {
                return ResponseEntity.ok(analytics);
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden: admin required"));
        }

        if (userId != null && !admin && (callerUid == null || !callerUid.equals(userId))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden: not the target user"));
        }

        return ResponseEntity.ok(activityService.getAnalyticsDashboard(startDate, endDate));
    }

    // S4-F11: Record Activity Lifecycle Event
    @PostMapping("/{id}/events")
    public ResponseEntity<Map<String, String>> recordLifecycleEvent(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        activityService.recordLifecycleEvent(id, body.get("status"), body.get("notes"));
        return ResponseEntity.status(201).body(Map.of("message", "Lifecycle event recorded"));
    }



}
