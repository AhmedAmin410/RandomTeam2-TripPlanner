package com.randmteam2.tripplanning.itinerary.controller;

import com.randmteam2.tripplanning.itinerary.dto.ItineraryDayRequest;
import com.randmteam2.tripplanning.itinerary.dto.ItineraryDetailsDTO;
import com.randmteam2.tripplanning.itinerary.dto.TripCostEstimateDTO;
import com.randmteam2.tripplanning.itinerary.dto.TripCostRequestDTO;
import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import com.randmteam2.tripplanning.itinerary.service.ItineraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Itinerary> cancelItinerary(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryService.cancelItinerary(id));
    }
    @PutMapping("/{itineraryId}/assign")
    public ResponseEntity<Itinerary> assignDestination(
            @PathVariable Long itineraryId,
            @RequestParam Long destinationId) {
        return ResponseEntity.ok(itineraryService.assignDestination(itineraryId, destinationId));
    }
    @PostMapping("/{itineraryId}/days")
    public ResponseEntity<Itinerary> addDays(@PathVariable Long itineraryId,
                                             @RequestBody List<ItineraryDayRequest> days) {
        return ResponseEntity.status(201).body(itineraryService.addDays(itineraryId, days));
    }
    @GetMapping("/{itineraryId}/details")
    public ResponseEntity<ItineraryDetailsDTO> getItineraryDetails(@PathVariable Long itineraryId) {
        return ResponseEntity.ok(itineraryService.getItineraryDetails(itineraryId));
    }
    @GetMapping("/search")
    public ResponseEntity<List<Itinerary>> search(
            @RequestParam(required = false) String status,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(itineraryService.searchByStatusAndDateRange(status, startDate, endDate));
    }

    @PostMapping("/estimate")
    public ResponseEntity<TripCostEstimateDTO> estimateTripCost(@RequestBody TripCostRequestDTO request) {
        return ResponseEntity.ok(itineraryService.estimateTripCost(request));
    }
}