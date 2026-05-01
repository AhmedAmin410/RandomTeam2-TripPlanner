package com.randomteam2.tripplanning.destination.controller;

import com.randomteam2.tripplanning.destination.dto.DestinationRateRequest;
import com.randomteam2.tripplanning.destination.dto.DestinationReviewAlertDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationStatusRequest;
import com.randomteam2.tripplanning.destination.dto.TopDestinationDTO;
import com.randomteam2.tripplanning.destination.dto.VerifyDestinationReviewRequest;
import com.randomteam2.tripplanning.destination.model.Destination;
import com.randomteam2.tripplanning.destination.service.DestinationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    private final DestinationService destinationService;

    public DestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }
    @PutMapping("/{id}/details")
    public ResponseEntity<Destination> updateDetails(
            @PathVariable Long id,
            @RequestBody Map<String, Object> details) {
        return ResponseEntity.ok(destinationService.updateDetails(id, details));
    }
    @PutMapping("/{id}/status")
    public ResponseEntity<Destination> updateStatus(
            @PathVariable Long id,
            @RequestBody DestinationStatusRequest body) {
        return ResponseEntity.ok(destinationService.updateStatus(id, body.getStatus()));
    }

    @GetMapping("/details/search")
    public ResponseEntity<List<Destination>> searchByDetails(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(destinationService.searchByDetailsKeyValue(key, value, status));
    }

    @GetMapping("/reports/top-rated")
    public ResponseEntity<List<TopDestinationDTO>> topRatedDestinations(@RequestParam int limit) {
        return ResponseEntity.ok(destinationService.getTopRatedDestinationsReport(limit));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<Destination> rateAfterVisit(
            @PathVariable Long id,
            @RequestBody DestinationRateRequest body) {
        return ResponseEntity.ok(destinationService.rateAfterVisit(id, body));
    }

    @PutMapping("/{destinationId}/reviews/{reviewId}/verify")
    public ResponseEntity<Destination> verifyDestinationReview(
            @PathVariable Long destinationId,
            @PathVariable Long reviewId,
            @RequestBody VerifyDestinationReviewRequest body) {
        return ResponseEntity.ok(
                destinationService.verifyDestinationReview(destinationId, reviewId, body));
    }

    @GetMapping("/reviews/low-rated")
    public ResponseEntity<List<DestinationReviewAlertDTO>> lowRatedReviews(
            @RequestParam int maxRating) {
        return ResponseEntity.ok(destinationService.getDestinationsWithLowRatedReviews(maxRating));
    }
}
