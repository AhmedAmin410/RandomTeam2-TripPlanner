package com.randmteam2.tripplanning.itinerary.controller;

import com.randmteam2.tripplanning.itinerary.dto.*;
import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import com.randmteam2.tripplanning.itinerary.model.ItineraryDay;
import com.randmteam2.tripplanning.itinerary.service.ItineraryDayService;
import com.randmteam2.tripplanning.itinerary.service.ItineraryService;
import com.randmteam2.tripplanning.itinerary.service.RecordVisitService;
import com.randmteam2.tripplanning.itinerary.security.JwtService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.randmteam2.tripplanning.itinerary.dto.ItineraryAnalyticsDashboardDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/itineraries")
public class ItineraryController {

    private final ItineraryService itineraryService;
    private final ItineraryDayService itineraryDayService;
    private final RecordVisitService recordVisitService;
    private final JwtService jwtService;

    public ItineraryController(ItineraryService itineraryService,
                               ItineraryDayService itineraryDayService, RecordVisitService recordVisitService,
                               JwtService jwtService) {
        this.itineraryService = itineraryService;
        this.itineraryDayService = itineraryDayService;
        this.recordVisitService = recordVisitService;
        this.jwtService = jwtService;
    }

    // S3-F12: Get Recommended Destinations for User
    @GetMapping("/recommendations")
    public ResponseEntity<?> recommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "5") int limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Ownership check: caller's uid must equal userId, or caller must be ADMIN.
        String token = authHeader != null ? authHeader.replace("Bearer ", "") : "";
        Long callerUid = jwtService.extractUserId(token);
        String role = jwtService.extractRole(token);
        if (!"ADMIN".equals(role) && (callerUid == null || !callerUid.equals(userId))) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: not the target user"));
        }
        try {
            return ResponseEntity.ok(itineraryService.getRecommendations(userId, limit));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "error";
            int status = msg.contains("not found") ? 404 : 400;
            return ResponseEntity.status(status).body(Map.of("error", msg));
        }
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

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<UserTripSummaryAggregateDTO> getUserSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(itineraryService.getUserTripSummary(userId));
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
            @RequestParam(required = false) Long destinationId,
            @RequestBody(required = false) java.util.Map<String, Long> body) {
        Long destId = destinationId != null ? destinationId :
                (body != null ? body.get("destinationId") : null);
        if (destId == null) {
            throw new IllegalArgumentException("destinationId is required");
        }
        return ResponseEntity.ok(itineraryService.assignDestination(itineraryId, destId));
    }
    @PostMapping("/{itineraryId}/days")
    public ResponseEntity<?> handleDays(@PathVariable Long itineraryId,
                                        @RequestBody com.fasterxml.jackson.databind.JsonNode body) {
        try {
            if (body.isArray()) {
                List<ItineraryDayRequest> days = new java.util.ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode node : body) {
                    ItineraryDayRequest req = new ItineraryDayRequest(
                            node.has("date") && !node.get("date").isNull() ?
                                    java.time.LocalDate.parse(node.get("date").asText()) : null,
                            node.has("title") ? node.get("title").asText() : null,
                            node.has("description") ? node.get("description").asText() : null,
                            null
                    );
                    days.add(req);
                }
                return ResponseEntity.ok(itineraryService.addDays(itineraryId, days));
            } else {
                ItineraryDay day = new ItineraryDay();
                if (body.has("dayOrder")) day.setDayOrder(body.get("dayOrder").asInt());
                if (body.has("date")) day.setDate(java.time.LocalDate.parse(body.get("date").asText()));
                if (body.has("title")) day.setTitle(body.get("title").asText());
                if (body.has("description") && !body.get("description").isNull())
                    day.setDescription(body.get("description").asText());
                if (body.has("status")) day.setStatus(
                        ItineraryDay.Status.valueOf(body.get("status").asText()));
                return ResponseEntity.status(201).body(itineraryDayService.create(itineraryId, day));
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid request: " + e.getMessage());
        }
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

    @GetMapping("/metadata/search")
    public ResponseEntity<List<Itinerary>> filterByMetadata(
            @RequestParam String key,
            @RequestParam String value) {
        return ResponseEntity.ok(itineraryService.filterByMetadata(key, value));
    }
    @GetMapping("/analytics")
    public ResponseEntity<ItineraryAnalyticsDTO> getAnalytics(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(itineraryService.getAnalytics(startDate, endDate));
    }
    @GetMapping("/{itineraryId}/days")
    public ResponseEntity<List<ItineraryDay>> getDays(@PathVariable Long itineraryId) {
        return ResponseEntity.ok(itineraryService.getDays(itineraryId));
    }
    @GetMapping("/analytics/dashboard")
    @Cacheable(value = "itinerary-service::S3-F10", key = "#startDate + '-' + #endDate")
    public ResponseEntity<ItineraryAnalyticsDashboardDTO> getAnalyticsDashboard(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(itineraryService.getAnalyticsDashboard(startDate, endDate));
    }
    @PostMapping("/{itineraryId}/record-visit")
    public ResponseEntity<?> recordVisit(@PathVariable Long itineraryId) {
        try {
            String result = recordVisitService.recordVisit(itineraryId);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/user/{userId}/active-count")
    public ResponseEntity<Integer> getUserActiveCount(@PathVariable Long userId) {
        return ResponseEntity.ok(itineraryService.getUserActiveCount(userId));
    }

    @GetMapping("/user/{userId}/completed-count")
    public ResponseEntity<Long> getUserCompletedCount(@PathVariable Long userId) {
        return ResponseEntity.ok(itineraryService.getUserCompletedCount(userId));
    }

    @GetMapping("/destination/{destinationId}/active-count")
    public ResponseEntity<Integer> getDestinationActiveCount(@PathVariable Long destinationId) {
        return ResponseEntity.ok(itineraryService.getDestinationActiveCount(destinationId));
    }

    @GetMapping("/destination/{destinationId}/booking-revenue")
    public ResponseEntity<DestinationBookingRevenueAggregateDTO> getDestinationBookingRevenue(
            @PathVariable Long destinationId) {
        return ResponseEntity.ok(itineraryService.getDestinationBookingRevenue(destinationId));
    }

    @GetMapping("/destination/{destinationId}/dashboard-aggregate")
    public ResponseEntity<DestinationDashboardAggregateDTO> getDestinationDashboardAggregate(
            @PathVariable Long destinationId) {
        return ResponseEntity.ok(itineraryService.getDestinationDashboardAggregate(destinationId));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ItinerarySummaryDTO>> getBatch(
            @RequestBody BatchItineraryRequest request) {
        return ResponseEntity.ok(itineraryService.getBatch(request.getItineraryIds()));
    }

}
