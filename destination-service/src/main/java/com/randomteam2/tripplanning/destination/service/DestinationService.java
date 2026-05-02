package com.randomteam2.tripplanning.destination.service;

import com.randomteam2.tripplanning.destination.dto.DestinationRateRequest;
import com.randomteam2.tripplanning.destination.dto.DestinationReviewAlertDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationRevenueDTO;
import com.randomteam2.tripplanning.destination.dto.TopDestinationDTO;
import com.randomteam2.tripplanning.destination.dto.VerifyDestinationReviewRequest;
import com.randomteam2.tripplanning.destination.model.Destination;
import com.randomteam2.tripplanning.destination.model.DestinationReview;
import com.randomteam2.tripplanning.destination.repository.DestinationRepository;
import com.randomteam2.tripplanning.destination.repository.DestinationReviewRepository;
import com.randomteam2.tripplanning.destination.model.Destination;
import com.randomteam2.tripplanning.destination.model.DestinationReview;
import com.randomteam2.tripplanning.destination.observer.EntityObserver;
import com.randomteam2.tripplanning.destination.observer.MongoEventLogger;
import com.randomteam2.tripplanning.destination.repository.DestinationRepository;
import com.randomteam2.tripplanning.destination.repository.DestinationReviewRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final DestinationReviewRepository destinationReviewRepository;
    private final DestinationCacheInvalidationService cacheInvalidationService;
    private final List<EntityObserver> observers = new ArrayList<>();

    public DestinationService(
            DestinationRepository destinationRepository,
            DestinationReviewRepository destinationReviewRepository,
            MongoEventLogger mongoEventLogger,
            DestinationCacheInvalidationService cacheInvalidationService) {
        this.destinationRepository = destinationRepository;
        this.destinationReviewRepository = destinationReviewRepository;
        this.cacheInvalidationService = cacheInvalidationService;
        register(mongoEventLogger);
    }

    public void register(EntityObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    private Map<String, Object> destinationPayload(Destination destination) {
        Map<String, Object> payload = new HashMap<>();
        if (destination != null) {
            payload.put("destinationId", destination.getId());
            payload.put("destinationName", destination.getName());
            payload.put("status", destination.getStatus() != null ? destination.getStatus().name() : null);
        }
        return payload;
    }
    @Transactional(readOnly = true)
    public DestinationRevenueDTO getDestinationRevenueSummary(
            Long destinationId,
            LocalDate startDate,
            LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate are required");
        }

        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must not be before startDate");
        }

        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        Object[] row = destinationRepository.findDestinationRevenueSummary(destinationId, startDate, endDate);

        if (row != null && row.length == 1 && row[0] instanceof Object[]) {
            row = (Object[]) row[0];
        }

        DestinationRevenueDTO dto = new DestinationRevenueDTO();
        dto.setDestinationId(destination.getId());
        dto.setName(destination.getName());
        dto.setTotalBookings(row != null && row[0] != null ? ((Number) row[0]).longValue() : 0L);
        dto.setTotalRevenue(row != null && row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
        dto.setAverageBookingAmount(row != null && row[2] != null ? ((Number) row[2]).doubleValue() : 0.0);

        return dto;
    }
    @Transactional
    public Destination updateDetails(Long id, Map<String, Object> incomingDetails) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        if (incomingDetails == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "details body is required");
        }

        Map<String, Object> mergedDetails = destination.getDetails();

        if (mergedDetails == null) {
            mergedDetails = new HashMap<>();
        } else {
            mergedDetails = new HashMap<>(mergedDetails);
        }

        mergedDetails.putAll(incomingDetails);

        destination.setDetails(mergedDetails);
        Destination savedDestination = destinationRepository.save(destination);
        cacheInvalidationService.evictDestinationCaches(savedDestination.getId());

        Map<String, Object> payload = destinationPayload(savedDestination);
        payload.put("updatedDetailKeys", incomingDetails.keySet());

        notifyObservers("DETAILS_UPDATED", payload);

        return savedDestination;
    }

    @Transactional
    public Destination updateStatus(Long id, String statusRaw) {
        if (statusRaw == null || statusRaw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }

        final Destination.Status newStatus;
        try {
            newStatus = Destination.Status.valueOf(statusRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }

        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        if (newStatus == Destination.Status.INACTIVE) {
            long activeRefs = destinationRepository.countActiveItinerariesReferencingDestination(id);
            if (activeRefs > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Active itineraries still reference this destination");
            }
        }

        Destination.Status oldStatus = destination.getStatus();
        destination.setStatus(newStatus);
        Destination savedDestination = destinationRepository.save(destination);
        cacheInvalidationService.evictDestinationCaches(savedDestination.getId());

        Map<String, Object> payload = destinationPayload(savedDestination);
        payload.put("oldStatus", oldStatus != null ? oldStatus.name() : null);
        payload.put("newStatus", newStatus.name());

        notifyObservers("STATUS_CHANGED", payload);

        return savedDestination;
    }

    @Transactional(readOnly = true)
    public List<Destination> searchByDetailsKeyValue(String key, String value, String statusRaw) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key and value are required");
        }

        String statusFilter = null;
        if (statusRaw != null && !statusRaw.isBlank()) {
            try {
                statusFilter = Destination.Status.valueOf(statusRaw.trim().toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
            }
        }

        return destinationRepository.searchByDetailsKeyValue(
                key.trim(),
                value.trim(),
                statusFilter);
    }

    @Transactional(readOnly = true)
    public List<TopDestinationDTO> getTopRatedDestinationsReport(int limit) {
        if (limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be at least 1");
        }

        List<Object[]> rows = destinationRepository.findTopRatedDestinationsReport(limit);
        List<TopDestinationDTO> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            TopDestinationDTO dto = new TopDestinationDTO();
            dto.setDestinationId(((Number) row[0]).longValue());
            dto.setName((String) row[1]);
            dto.setRating(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0);
            dto.setTotalBookings(((Number) row[3]).longValue());
            result.add(dto);
        }
        return result;
    }

    @Transactional
    public Destination rateAfterVisit(Long destinationId, DestinationRateRequest request) {
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        if (request == null || request.getItineraryId() == null || request.getRating() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itineraryId and rating are required");
        }

        int ratingValue = request.getRating();
        if (ratingValue < 1 || ratingValue > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating must be between 1 and 5");
        }

        List<Object[]> rows = destinationRepository.findItineraryDestinationIdAndStatus(request.getItineraryId());
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }

        Object[] row = rows.get(0);
        Long itineraryDestinationId = row[0] != null ? ((Number) row[0]).longValue() : null;
        String status = row[1] != null ? row[1].toString() : null;

        if (itineraryDestinationId == null || !itineraryDestinationId.equals(destinationId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Itinerary does not reference this destination");
        }
        if (!"COMPLETED".equals(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Itinerary must be COMPLETED to rate this destination");
        }

        int priorCount = destination.getTotalRatings() != null ? destination.getTotalRatings() : 0;
        double priorAvg = destination.getRating() != null ? destination.getRating() : 0.0;
        int newCount = priorCount + 1;
        double newAvg = (priorAvg * priorCount + ratingValue) / newCount;

        destination.setRating(newAvg);
        destination.setTotalRatings(newCount);
        Destination savedDestination = destinationRepository.save(destination);
        cacheInvalidationService.evictDestinationCaches(savedDestination.getId());

        Map<String, Object> payload = destinationPayload(savedDestination);
        payload.put("itineraryId", request.getItineraryId());
        payload.put("ratingValue", ratingValue);
        payload.put("previousAverageRating", priorAvg);
        payload.put("newAverageRating", newAvg);
        payload.put("previousTotalRatings", priorCount);
        payload.put("newTotalRatings", newCount);

        notifyObservers("RATING_ADDED", payload);

        return savedDestination;
    }

    @Transactional
    public Destination verifyDestinationReview(
            Long destinationId,
            Long reviewId,
            VerifyDestinationReviewRequest request) {
        destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        if (request == null || request.getVerifiedBy() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verifiedBy is required");
        }

        long adminCount = destinationRepository.countAdminUserById(request.getVerifiedBy());
        if (adminCount == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an ADMIN user may verify reviews");
        }

        DestinationReview review = destinationReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

        if (review.getDestination() == null || !destinationId.equals(review.getDestination().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Review does not belong to this destination");
        }

        LocalDate visitDate = review.getVisitDate();
        if (visitDate != null && visitDate.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot verify a review for a future visit date");
        }

        review.setVerified(true);
        Map<String, Object> metadata = review.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        } else {
            metadata = new HashMap<>(metadata);
        }
        metadata.put("verifiedAt", LocalDateTime.now().toString());
        metadata.put("verifiedBy", request.getVerifiedBy());
        review.setMetadata(metadata);

        destinationReviewRepository.save(review);

        Destination destinationWithReviews = destinationRepository.findByIdWithDestinationReviews(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
        cacheInvalidationService.evictDestinationReviewCaches(destinationId, reviewId);

        Map<String, Object> payload = destinationPayload(destinationWithReviews);
        payload.put("reviewId", reviewId);
        payload.put("verifiedBy", request.getVerifiedBy());
        payload.put("verifiedAt", metadata.get("verifiedAt"));

        notifyObservers("REVIEW_VERIFIED", payload);

        return destinationWithReviews;
    }


    @Transactional(readOnly = true)
    public List<DestinationReviewAlertDTO> getDestinationsWithLowRatedReviews(int maxRating) {
        if (maxRating < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxRating must not be negative");
        }

        List<DestinationReview> reviews =
                destinationReviewRepository.findLowRatedReviewsWithDestination(maxRating);
        if (reviews.isEmpty()) {
            return List.of();
        }

        Map<Long, List<DestinationReview>> byDestination = new LinkedHashMap<>();
        for (DestinationReview review : reviews) {
            Destination dest = review.getDestination();
            if (dest == null || dest.getId() == null) {
                continue;
            }
            byDestination.computeIfAbsent(dest.getId(), id -> new ArrayList<>()).add(review);
        }

        List<DestinationReviewAlertDTO> result = new ArrayList<>(byDestination.size());
        for (List<DestinationReview> group : byDestination.values()) {
            Destination destination = group.get(0).getDestination();
            DestinationReviewAlertDTO dto = new DestinationReviewAlertDTO();
            dto.setDestinationId(destination.getId());
            dto.setDestinationName(destination.getName());
            dto.setDestinationStatus(destination.getStatus().name());
            dto.setLowRatedReviews(group);
            dto.setLowRatedCount(group.size());
            result.add(dto);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Destination> searchByCategoryAndRatingRange(String category, Double minRating, Double maxRating) {
        if (minRating == null || maxRating == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRating and maxRating are required");
        }

        if (minRating > maxRating) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRating cannot be greater than maxRating");
        }

        return destinationRepository.searchByCategoryAndRatingRange(category, minRating, maxRating);
    }
}
