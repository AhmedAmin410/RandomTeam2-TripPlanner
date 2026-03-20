package com.randmteam2.tripplanning.itinerary.controller;

import com.randmteam2.tripplanning.itinerary.model.Activity;
import com.randmteam2.tripplanning.itinerary.service.ActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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

    @GetMapping("/{id}")
    public ResponseEntity<Activity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.getById(id));
    }

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
}