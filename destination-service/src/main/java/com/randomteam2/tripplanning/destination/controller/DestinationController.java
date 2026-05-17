package com.randomteam2.tripplanning.destination.controller;

import com.randomteam2.tripplanning.destination.dto.*;
import com.randomteam2.tripplanning.destination.model.Destination;
import com.randomteam2.tripplanning.destination.model.DestinationReview;
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

    @PostMapping("/batch")
    public ResponseEntity<List<DestinationSummaryDTO>> getDestinationsBatch(
            @RequestBody DestinationBatchRequest request) {
        return ResponseEntity.ok(destinationService.getDestinationsBatch(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DestinationDTO> getDestinationById(@PathVariable Long id) {
        return ResponseEntity.ok(destinationService.getDestinationDTOById(id));
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

    // ─── CRUD: DestinationReview ─────────────────────────────────────────────

    @PostMapping("/{destinationId}/reviews")
    public ResponseEntity<DestinationReview> createReview(@PathVariable Long destinationId,
                                                          @RequestBody DestinationReview review) {
        return ResponseEntity.status(HttpStatus.CREATED).body(destinationService.createReview(destinationId, review));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<DestinationReview>> getAllReviews() {
        return ResponseEntity.ok(destinationService.getAllReviews());
    }

    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<DestinationReview> getReviewById(@PathVariable Long reviewId) {
        return ResponseEntity.ok(destinationService.getReviewById(reviewId));
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<DestinationReview> updateReview(@PathVariable Long reviewId,
                                                          @RequestBody DestinationReview review) {
        return ResponseEntity.ok(destinationService.updateReview(reviewId, review));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        destinationService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    // ─── M1 Features ─────────────────────────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<List<Destination>> searchByCategoryAndRatingRange(
            @RequestParam(required = false) String category,
            @RequestParam Double minRating,
            @RequestParam Double maxRating) {
        return ResponseEntity.ok(destinationService.searchByCategoryAndRatingRange(category, minRating, maxRating));
    }

    @PutMapping("/{id}/details")
    public ResponseEntity<Destination> updateDetails(@PathVariable Long id,
                                                     @RequestBody Map<String, Object> details) {
        return ResponseEntity.ok(destinationService.updateDetails(id, details));
    }

    /** S2-F3: Get Destination Booking Revenue Summary (M3: single Feign call to itinerary-service) */
    @GetMapping("/{id}/revenue")
    public ResponseEntity<DestinationRevenueDTO> getDestinationRevenue(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(destinationService.getDestinationRevenueSummary(id, startDate, endDate));
    }

    /** S2-F4: Update Destination Status (M3: INACTIVE guard via Feign active-count) */
    @PutMapping("/{id}/status")
    public ResponseEntity<Destination> updateStatus(@PathVariable Long id,
                                                    @RequestBody DestinationStatusRequest body) {
        return ResponseEntity.ok(destinationService.updateStatus(id, body.getStatus()));
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

    @GetMapping("/reviews/low-rated")
    public ResponseEntity<List<DestinationReviewAlertDTO>> lowRatedReviews(@RequestParam int maxRating) {
        return ResponseEntity.ok(destinationService.getDestinationsWithLowRatedReviews(maxRating));
    }

    // ─── M2 Features ─────────────────────────────────────────────────────────

    /** S2-F10: Full-text search via Elasticsearch */
    @GetMapping("/search/full-text")
    public ResponseEntity<List<DestinationSearchResultDTO>> fullTextSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating) {
        return ResponseEntity.ok(destinationService.fullTextSearch(query, category, status, minRating, maxRating));
    }

    /** S2-F11: Index a destination into Elasticsearch */
    @PostMapping("/{id}/index")
    public ResponseEntity<Void> indexDestination(@PathVariable Long id) {
        destinationService.indexDestination(id);
        return ResponseEntity.ok().build();
    }

    /** S2-F12: Get Destination Analytics Dashboard */
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<DestinationDashboardDTO> getDestinationDashboard(@PathVariable Long id) {
        return ResponseEntity.ok(destinationService.getDestinationDashboard(id));
    }

}