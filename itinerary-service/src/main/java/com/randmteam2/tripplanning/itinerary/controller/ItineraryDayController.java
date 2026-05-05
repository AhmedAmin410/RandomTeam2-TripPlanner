package com.randmteam2.tripplanning.itinerary.controller;

import com.randmteam2.tripplanning.itinerary.model.ItineraryDay;
import com.randmteam2.tripplanning.itinerary.service.ItineraryDayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/itinerary-days")
public class ItineraryDayController {

    private final ItineraryDayService itineraryDayService;

    public ItineraryDayController(ItineraryDayService itineraryDayService) {
        this.itineraryDayService = itineraryDayService;
    }


    @PostMapping("/itinerary/{itineraryId}")
    public ResponseEntity<ItineraryDay> create(@PathVariable Long itineraryId,
                                               @RequestBody ItineraryDay day) {
        return ResponseEntity.status(201).body(itineraryDayService.create(itineraryId, day));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItineraryDay> getById(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryDayService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ItineraryDay>> getAll() {
        return ResponseEntity.ok(itineraryDayService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItineraryDay> update(@PathVariable Long id,
                                               @RequestBody ItineraryDay day) {
        return ResponseEntity.ok(itineraryDayService.update(id, day));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itineraryDayService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
