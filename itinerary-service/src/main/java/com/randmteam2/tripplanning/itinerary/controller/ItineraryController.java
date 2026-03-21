package com.randmteam2.tripplanning.itinerary.controller;

import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import com.randmteam2.tripplanning.itinerary.service.ItineraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/itineraries")
public class ItineraryController {

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<Itinerary> completeItinerary(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryService.completeItinerary(id));
    }

    @PostMapping
    public ResponseEntity<Itinerary> create(@RequestBody Itinerary itinerary) {
        return ResponseEntity.status(201).body(itineraryService.create(itinerary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Itinerary> getById(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<Itinerary>> getAll() {
        return ResponseEntity.ok(itineraryService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Itinerary> update(@PathVariable Long id, @RequestBody Itinerary itinerary) {
        return ResponseEntity.ok(itineraryService.update(id, itinerary));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itineraryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}