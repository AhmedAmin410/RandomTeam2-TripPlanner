package com.randomteam2.tripplanning.destination.controller;

import com.randomteam2.tripplanning.destination.dto.DestinationReviewRequest;
import com.randomteam2.tripplanning.destination.dto.DestinationReviewResponse;
import com.randomteam2.tripplanning.destination.dto.DestinationReviewUpdateRequest;
import com.randomteam2.tripplanning.destination.model.DestinationReview;
import com.randomteam2.tripplanning.destination.service.DestinationReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for destination reviews (CRUD and listing by destination).
 */
@RestController
@RequestMapping("/api")
public class DestinationReviewController {

    private final DestinationReviewService destinationReviewService;

    public DestinationReviewController(DestinationReviewService destinationReviewService) {
        this.destinationReviewService = destinationReviewService;
    }

    @PostMapping("/destinations/{destinationId}/reviews")
    public ResponseEntity<DestinationReviewResponse> createReview(
            @PathVariable Long destinationId,
            @Valid @RequestBody DestinationReviewRequest request) {
        DestinationReview saved = destinationReviewService.createReview(destinationId, toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(DestinationReviewResponse.fromEntity(saved));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<DestinationReviewResponse>> getAllReviews() {
        List<DestinationReviewResponse> list = destinationReviewService.getAllReviews().stream()
                .map(DestinationReviewResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/reviews/{id}")
    public ResponseEntity<DestinationReviewResponse> getReviewById(@PathVariable Long id) {
        return ResponseEntity.ok(DestinationReviewResponse.fromEntity(destinationReviewService.getReviewById(id)));
    }

    @GetMapping("/destinations/{destinationId}/reviews")
    public ResponseEntity<List<DestinationReviewResponse>> getReviewsByDestination(@PathVariable Long destinationId) {
        List<DestinationReviewResponse> list = destinationReviewService.getReviewsByDestination(destinationId).stream()
                .map(DestinationReviewResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PutMapping("/reviews/{id}")
    public ResponseEntity<DestinationReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody DestinationReviewUpdateRequest request) {
        DestinationReview updated = destinationReviewService.updateReview(id, request);
        return ResponseEntity.ok(DestinationReviewResponse.fromEntity(updated));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        destinationReviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    private static DestinationReview toEntity(DestinationReviewRequest request) {
        DestinationReview entity = new DestinationReview();
        entity.setType(request.getType());
        entity.setContent(request.getContent());
        entity.setRating(request.getRating());
        entity.setVisitDate(request.getVisitDate());
        entity.setVerified(request.getVerified() != null ? request.getVerified() : false);
        entity.setMetadata(request.getMetadata());
        return entity;
    }
}
