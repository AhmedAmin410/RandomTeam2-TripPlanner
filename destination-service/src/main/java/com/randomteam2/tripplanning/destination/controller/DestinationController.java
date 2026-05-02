package com.randomteam2.tripplanning.destination.controller;

import com.randomteam2.tripplanning.destination.dto.*;
import com.randomteam2.tripplanning.destination.model.Destination;
import com.randomteam2.tripplanning.destination.service.DestinationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    private final DestinationService destinationService;

    public DestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    // ─── CRUD: Destination ────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Destination> createDestination(@RequestBody Destination destination) {
        return ResponseEntity.status(HttpStatus.CREATED).body(destinationService.createDestination(destination));
    }

    @GetMapping
    public ResponseEntity<List<Destination>> getAllDestinations() {
        return ResponseEntity.ok(destinationService.getAllDestinations());
    }

    /**
     * S2-F1: optional category and/or rating bounds; results sorted by rating (highest first).
     * Declared before {@code /{id}} so the literal path is not captured as an id.
     */
    @GetMapping("/search")
    public ResponseEntity<List<Destination>> searchDestinations(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating) {
        return ResponseEntity.ok(destinationService.searchDestinations(category, minRating, maxRating));
    }

    @GetMapping("/details/search")
    public ResponseEntity<List<Destination>> searchByDetails(@RequestParam String key,
                                                             @RequestParam String value,
                                                             @RequestParam(required = false) String status) {
        return ResponseEntity.ok(destinationService.searchByDetailsKeyValue(key, value, status));
    }

    @GetMapping("/reports/top-rated")
    public ResponseEntity<List<TopDestinationDTO>> topRatedDestinations(@RequestParam int limit) {
        return ResponseEntity.ok(destinationService.getTopRatedDestinationsReport(limit));
    }

    @GetMapping("/reviews/low-rated")
    public ResponseEntity<List<DestinationReviewAlertDTO>> lowRatedReviews(@RequestParam int maxRating) {
        return ResponseEntity.ok(destinationService.getDestinationsWithLowRatedReviews(maxRating));
    }

    @GetMapping("/search/full-text")
    public ResponseEntity<List<DestinationSearchResultDTO>> fullTextSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating) {
        return ResponseEntity.ok(destinationService.fullTextSearch(query, category, status, minRating, maxRating));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Destination> getDestinationById(@PathVariable Long id) {
        return ResponseEntity.ok(destinationService.getDestinationById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Destination> updateDestination(@PathVariable Long id, @RequestBody Destination destination) {
        return ResponseEntity.ok(destinationService.updateDestination(id, destination));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDestination(@PathVariable Long id) {
        destinationService.deleteDestination(id);
        return ResponseEntity.noContent().build();
    }

    // ─── M1 Features ─────────────────────────────────────────────────────────

    @PutMapping("/{id}/details")
    public ResponseEntity<Destination> updateDetails(@PathVariable Long id,
                                                     @RequestBody Map<String, Object> details) {
        return ResponseEntity.ok(destinationService.updateDetails(id, details));
    }

    @GetMapping("/{id}/revenue")
    public ResponseEntity<DestinationRevenueDTO> getDestinationRevenue(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(destinationService.getDestinationRevenueSummary(id, startDate, endDate));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Destination> updateStatus(@PathVariable Long id,
                                                    @RequestBody DestinationStatusRequest body) {
        return ResponseEntity.ok(destinationService.updateStatus(id, body.getStatus()));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<Destination> rateAfterVisit(@PathVariable Long id,
                                                      @RequestBody DestinationRateRequest body) {
        return ResponseEntity.ok(destinationService.rateAfterVisit(id, body));
    }

    @PutMapping("/{destinationId}/reviews/{reviewId}/verify")
    public ResponseEntity<Destination> verifyDestinationReview(@PathVariable Long destinationId,
                                                               @PathVariable Long reviewId,
                                                               @RequestBody VerifyDestinationReviewRequest body) {
        return ResponseEntity.ok(destinationService.verifyDestinationReview(destinationId, reviewId, body));
    }

    @PostMapping("/{id}/index")
    public ResponseEntity<Void> indexDestination(@PathVariable Long id) {
        destinationService.indexDestination(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<DestinationDashboardDTO> getDestinationDashboard(@PathVariable Long id) {
        return ResponseEntity.ok(destinationService.getDestinationDashboard(id));
    }

}