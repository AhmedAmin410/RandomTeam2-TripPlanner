package com.randomteam2.tripplanning.destination.service;

import com.randomteam2.tripplanning.destination.adapter.ElasticsearchHitAdapter;
import com.randomteam2.tripplanning.destination.adapter.ObjectArrayDtoAdapter;
import com.randomteam2.tripplanning.destination.dto.*;
import com.randomteam2.tripplanning.destination.elasticsearch.DestinationSearchDocument;
import com.randomteam2.tripplanning.destination.elasticsearch.DestinationSearchRepository;
import com.randomteam2.tripplanning.destination.model.Destination;
import com.randomteam2.tripplanning.destination.model.DestinationReview;
import com.randomteam2.tripplanning.destination.observer.EntityObserver;
import com.randomteam2.tripplanning.destination.observer.MongoEventLogger;
import com.randomteam2.tripplanning.destination.feign.ItineraryServiceClient;
import com.randomteam2.tripplanning.destination.feign.UserServiceClient;
import com.randomteam2.tripplanning.destination.messaging.DestinationEventPublisher;
import com.randomteam2.tripplanning.destination.repository.DestinationEventRepository;
import com.randomteam2.tripplanning.destination.repository.DestinationRepository;
import com.randomteam2.tripplanning.destination.repository.DestinationReviewRepository;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DestinationService {

    private static final Logger logger = LoggerFactory.getLogger(DestinationService.class);

    private final DestinationRepository destinationRepository;
    private final DestinationReviewRepository destinationReviewRepository;
    private final DestinationCacheInvalidationService cacheInvalidationService;
    private final ElasticsearchIndexService elasticsearchIndexService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchHitAdapter elasticsearchHitAdapter;
    private final ObjectArrayDtoAdapter objectArrayDtoAdapter;
    private final DestinationEventRepository destinationEventRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ItineraryServiceClient itineraryServiceClient;
    private final UserServiceClient userServiceClient;
    private final DestinationEventPublisher destinationEventPublisher;
    private final List<EntityObserver> observers = new ArrayList<>();

    public DestinationService(
            DestinationRepository destinationRepository,
            DestinationReviewRepository destinationReviewRepository,
            MongoEventLogger mongoEventLogger,
            DestinationCacheInvalidationService cacheInvalidationService,
            ElasticsearchIndexService elasticsearchIndexService,
            ElasticsearchOperations elasticsearchOperations,
            ElasticsearchHitAdapter elasticsearchHitAdapter,
            ObjectArrayDtoAdapter objectArrayDtoAdapter,
            DestinationEventRepository destinationEventRepository,
            RedisTemplate<String, Object> redisTemplate,
            ItineraryServiceClient itineraryServiceClient,
            UserServiceClient userServiceClient,
            DestinationEventPublisher destinationEventPublisher) {
        this.destinationRepository = destinationRepository;
        this.destinationReviewRepository = destinationReviewRepository;
        this.cacheInvalidationService = cacheInvalidationService;
        this.elasticsearchIndexService = elasticsearchIndexService;
        this.elasticsearchOperations = elasticsearchOperations;
        this.elasticsearchHitAdapter = elasticsearchHitAdapter;
        this.objectArrayDtoAdapter = objectArrayDtoAdapter;
        this.destinationEventRepository = destinationEventRepository;
        this.redisTemplate = redisTemplate;
        this.itineraryServiceClient = itineraryServiceClient;
        this.userServiceClient = userServiceClient;
        this.destinationEventPublisher = destinationEventPublisher;
        register(mongoEventLogger);
    }

    // ─── Observer pattern ────────────────────────────────────────────────────

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

    // ─── CRUD operations ─────────────────────────────────────────────────────

    @Transactional
    public Destination createDestination(Destination destination) {
        Destination saved = destinationRepository.save(destination);
        // Auto-index to Elasticsearch on CRUD create
        elasticsearchIndexService.indexDestination(saved, "auto_crud_create");
        cacheInvalidationService.evictDestinationCaches(saved.getId());
        notifyObservers("DESTINATION_CREATED", destinationPayload(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Destination> getAllDestinations() {
        return destinationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DestinationSummaryDTO> getDestinationsBatch(DestinationBatchRequest request) {
        if (request == null || request.destinationIds() == null || request.destinationIds().isEmpty()) {
            return List.of();
        }
        List<Long> ids = request.destinationIds();
        Map<Long, Destination> byId = destinationRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Destination::getId, d -> d, (a, b) -> a, LinkedHashMap::new));
        List<DestinationSummaryDTO> result = new ArrayList<>();
        for (Long id : ids) {
            Destination destination = byId.get(id);
            if (destination != null) {
                result.add(new DestinationSummaryDTO(
                        destination.getId(),
                        destination.getName(),
                        destination.getCountry(),
                        destination.getCategory() != null ? destination.getCategory().name() : null));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Destination getDestinationById(Long id) {
        String cacheKey = "destination-service::destination::" + id;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Destination dest) {
                return dest;
            }
        } catch (Exception e) {
            logger.warn("Redis read failed for key {}", cacheKey, e);
        }
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
        try {
            redisTemplate.opsForValue().set(cacheKey, destination, 15, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Redis write failed for key {}", cacheKey, e);
        }
        return destination;
    }

    /**
     * Returns the clean DestinationDTO used by GET /api/destinations/{id}.
     * Called by S3 and S5 via Feign — returns exactly:
     * id, name, country, category, status, rating, totalRatings, details.
     */
    @Transactional(readOnly = true)
    public DestinationDTO getDestinationDTOById(Long id) {
        Destination destination = getDestinationById(id);
        return toDTO(destination);
    }

    private DestinationDTO toDTO(Destination d) {
        return new DestinationDTO(
                d.getId(),
                d.getName(),
                d.getCountry(),
                d.getCategory() != null ? d.getCategory().name() : null,
                d.getStatus() != null ? d.getStatus().name() : null,
                d.getRating(),
                d.getTotalRatings(),
                d.getDetails()
        );
    }


    @Transactional
    public Destination updateDestination(Long id, Destination updated) {
        Destination existing = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getCountry() != null) existing.setCountry(updated.getCountry());
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getCategory() != null) existing.setCategory(updated.getCategory());
        if (updated.getStatus() != null) existing.setStatus(updated.getStatus());
        if (updated.getRating() != null) existing.setRating(updated.getRating());
        if (updated.getTotalRatings() != null) existing.setTotalRatings(updated.getTotalRatings());
        if (updated.getDetails() != null) existing.setDetails(updated.getDetails());
        Destination saved = destinationRepository.save(existing);
        // Auto-index to Elasticsearch on CRUD update
        elasticsearchIndexService.indexDestination(saved, "auto_crud_update");
        cacheInvalidationService.evictDestinationCaches(saved.getId());
        notifyObservers("DESTINATION_UPDATED", destinationPayload(saved));
        return saved;
    }

    @Transactional
    public void deleteDestination(Long id) {
        Destination existing = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
        // Remove from Elasticsearch before deleting from PG
        elasticsearchIndexService.deleteDestination(id);
        destinationRepository.deleteById(id);
        cacheInvalidationService.evictDestinationCaches(id);
        notifyObservers("DESTINATION_DELETED", destinationPayload(existing));
    }

    // ─── Review CRUD ─────────────────────────────────────────────────────────

    @Transactional
    public DestinationReview createReview(Long destinationId, DestinationReview review) {
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
        review.setDestination(destination);
        DestinationReview saved = destinationReviewRepository.save(review);
        cacheInvalidationService.evictDestinationCaches(destinationId);
        notifyObservers("REVIEW_CREATED", destinationPayload(destination));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<DestinationReview> getAllReviews() {
        return destinationReviewRepository.findAll();
    }

    @Transactional(readOnly = true)
    public DestinationReview getReviewById(Long reviewId) {
        String cacheKey = "destination-service::destination-review::" + reviewId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof DestinationReview r) {
                return r;
            }
        } catch (Exception e) {
            logger.warn("Redis read failed for key {}", cacheKey, e);
        }
        DestinationReview review = destinationReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        try {
            redisTemplate.opsForValue().set(cacheKey, review, 15, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Redis write failed for key {}", cacheKey, e);
        }
        return review;
    }

    @Transactional
    public DestinationReview updateReview(Long reviewId, DestinationReview updated) {
        DestinationReview existing = destinationReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        if (updated.getContent() != null) existing.setContent(updated.getContent());
        if (updated.getRating() != null) existing.setRating(updated.getRating());
        if (updated.getVisitDate() != null) existing.setVisitDate(updated.getVisitDate());
        if (updated.getType() != null) existing.setType(updated.getType());
        if (updated.getMetadata() != null) existing.setMetadata(updated.getMetadata());
        DestinationReview saved = destinationReviewRepository.save(existing);
        cacheInvalidationService.evictDestinationReviewCaches(
                existing.getDestination() != null ? existing.getDestination().getId() : null, reviewId);
        return saved;
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        DestinationReview review = destinationReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        Long destId = review.getDestination() != null ? review.getDestination().getId() : null;
        destinationReviewRepository.deleteById(reviewId);
        cacheInvalidationService.evictDestinationReviewCaches(destId, reviewId);
    }

    // ─── M1 Features ─────────────────────────────────────────────────────────

    /**
     * S2-F3: Get Destination Booking Revenue Summary.
     *
     * M3 change: the M1 implementation joined destinations, itineraries, and bookings
     * across all three databases in a single native SQL query. That cross-database JOIN is
     * replaced here with a single Feign call to itinerary-service, which encapsulates the
     * chain (itinerary-service → booking-service) and returns the pre-aggregated result.
     * destination-service never opens a JDBC connection to itinerary-postgres or
     * booking-postgres.
     *
     * The returned aggregate is then mapped to DestinationRevenueDTO via the
     * ObjectArrayDtoAdapter (M2 Builder contract preserved).
     *
     * Cache: 10-minute TTL, keyed by destinationId + date range.
     */
    @Transactional(readOnly = true)
    public DestinationRevenueDTO getDestinationRevenueSummary(Long destinationId,
                                                              LocalDate startDate,
                                                              LocalDate endDate) {
        // 1. Validate date parameters
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must not be before startDate");
        }

        // 2. Return cached result if present
        String cacheKey = "destination-service::S2-F3::" + destinationId + "::" + startDate + "::" + endDate;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof DestinationRevenueDTO hit) {
                return hit;
            }
        } catch (Exception e) {
            logger.warn("Redis read failed for S2-F3 key {}", cacheKey, e);
        }

        // 3. Validate destination exists — 404 if not found
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        // 4. Single Feign call to itinerary-service (M3 — replaces 3-table JOIN)
        DestinationBookingRevenueAggregateDTO aggregate = itineraryServiceClient.getDestinationBookingRevenue(
                destinationId, startDate.toString(), endDate.toString());

        // 5. Adapt aggregate + local destination fields into the response DTO
        DestinationRevenueDTO dto = objectArrayDtoAdapter.adaptRevenue(
                destination.getId(), destination.getName(), aggregate);

        // 6. Cache for 10 minutes
        try {
            redisTemplate.opsForValue().set(cacheKey, dto, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Redis write failed for S2-F3 key {}", cacheKey, e);
        }

        return dto;
    }

    @Transactional
    public Destination updateDetails(Long id, Map<String, Object> incomingDetails) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
        if (incomingDetails == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "details body is required");
        }
        Map<String, Object> mergedDetails = destination.getDetails() == null ? new HashMap<>() : new HashMap<>(destination.getDetails());
        mergedDetails.putAll(incomingDetails);
        destination.setDetails(mergedDetails);
        Destination savedDestination = destinationRepository.save(destination);
        elasticsearchIndexService.indexDestination(savedDestination, "auto_crud_update");
        cacheInvalidationService.evictDestinationCaches(savedDestination.getId());
        Map<String, Object> payload = destinationPayload(savedDestination);
        payload.put("updatedDetailKeys", incomingDetails.keySet());
        notifyObservers("DETAILS_UPDATED", payload);
        return savedDestination;
    }

    /**
     * S2-F4: Update Destination Status.
     *
     * M3 change: the M1 INACTIVE guard used a direct SQL COUNT on the shared database
     * (SELECT COUNT(*) FROM itineraries WHERE destination_id = ? AND status IN ('DRAFT','PLANNED','IN_PROGRESS')).
     * That is replaced with a single Feign call to itinerary-service at
     * GET /api/itineraries/destination/{destinationId}/active-count which counts over the
     * expanded M3 active set: DRAFT, PLANNED, IN_PROGRESS, COMPLETING, PAYMENT_PENDING.
     * The saga states (COMPLETING, PAYMENT_PENDING) are included so a destination cannot
     * be deactivated while any trip referencing it is mid-saga.
     *
     * For ACTIVE or SEASONAL transitions the Feign call is skipped entirely (same as M1).
     *
     * After every successful status change a destination.status-changed event is published
     * to the destination.events RabbitMQ exchange.
     */
    @Transactional
    public Destination updateStatus(Long id, String statusRaw) {
        // 1. Reject blank or null input
        if (statusRaw == null || statusRaw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }

        // 2. Parse to enum — reject unknown values with 400
        final Destination.Status newStatus;
        try {
            newStatus = Destination.Status.valueOf(statusRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }

        // 3. Fetch destination — 404 if absent
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        // 4. INACTIVE guard (M3): single Feign call replaces the M1 direct SQL COUNT.
        //    ACTIVE and SEASONAL transitions skip this block entirely.
        if (newStatus == Destination.Status.INACTIVE) {
            Integer activeCount = itineraryServiceClient.getDestinationActiveItineraryCount(id);
            if (activeCount != null && activeCount > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot mark destination INACTIVE: " + activeCount +
                                " active itinerary/itineraries still reference it");
            }
        }

        // 5. Apply change and persist
        Destination.Status oldStatus = destination.getStatus();
        destination.setStatus(newStatus);
        Destination saved = destinationRepository.save(destination);

        // 6. Side-effects: Elasticsearch re-index, cache eviction, observer log
        elasticsearchIndexService.indexDestination(saved, "auto_crud_update");
        cacheInvalidationService.evictDestinationCaches(saved.getId());
        Map<String, Object> payload = destinationPayload(saved);
        payload.put("oldStatus", oldStatus != null ? oldStatus.name() : null);
        payload.put("newStatus", newStatus.name());
        notifyObservers("STATUS_CHANGED", payload);

        // 7. Publish destination.status-changed event to destination.events exchange
        destinationEventPublisher.publishStatusChanged(new StatusChangedEvent(
                id,
                oldStatus != null ? oldStatus.name() : null,
                newStatus.name()));

        return saved;
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

        String cacheKey = "destination-service::S2-F5::" + key + "::" + value + "::" + statusFilter;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                List<Destination> list = (List<Destination>) cached;
                return list;
            }
        } catch (Exception e) {
            logger.warn("Redis read failed", e);
        }

        List<Destination> result = destinationRepository.searchByDetailsKeyValue(key.trim(), value.trim(), statusFilter);
        try {
            redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Redis write failed", e);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<TopDestinationDTO> getTopRatedDestinationsReport(int limit) {
        if (limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be at least 1");
        }
        String cacheKey = "destination-service::S2-F6::" + limit;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                List<TopDestinationDTO> list = (List<TopDestinationDTO>) cached;
                return list;
            }
        } catch (Exception e) {
            logger.warn("Redis read failed", e);
        }

        List<Object[]> rows = destinationRepository.findTopRatedDestinationsReport(limit);
        List<TopDestinationDTO> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(TopDestinationDTO.builder()
                    .destinationId(((Number) row[0]).longValue())
                    .name((String) row[1])
                    .rating(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                    .totalBookings(((Number) row[3]).longValue())
                    .build());
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, result, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Redis write failed", e);
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
        ItineraryDTO itinerary = fetchItinerary(request.getItineraryId());
        if (itinerary.destinationId() == null || !itinerary.destinationId().equals(destinationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Itinerary does not reference this destination");
        }
        String status = itinerary.status() != null ? itinerary.status().trim().toUpperCase() : "";
        if (!"COMPLETED".equals(status) && !"PAID".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Itinerary must be in a completed state (COMPLETED or PAID) to rate this destination");
        }
        int priorCount = destination.getTotalRatings() != null ? destination.getTotalRatings() : 0;
        double priorAvg = destination.getRating() != null ? destination.getRating() : 0.0;
        int newCount = priorCount + 1;
        double newAvg = (priorAvg * priorCount + ratingValue) / newCount;
        destination.setRating(newAvg);
        destination.setTotalRatings(newCount);
        Destination savedDestination = destinationRepository.save(destination);
        elasticsearchIndexService.indexDestination(savedDestination, "auto_crud_update");
        cacheInvalidationService.evictDestinationCaches(savedDestination.getId());
        Map<String, Object> payload = destinationPayload(savedDestination);
        payload.put("itineraryId", request.getItineraryId());
        payload.put("ratingValue", ratingValue);
        notifyObservers("RATING_ADDED", payload);
        destinationEventPublisher.publishRated(new DestinationRatedEvent(
                destinationId,
                request.getItineraryId(),
                (double) ratingValue,
                itinerary.userId()));
        return savedDestination;
    }

    @Transactional
    public Destination verifyDestinationReview(Long destinationId, Long reviewId, VerifyDestinationReviewRequest request) {
        destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
        if (request == null || request.getVerifiedBy() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verifiedBy is required");
        }
        DestinationReview review = destinationReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        if (review.getDestination() == null || !destinationId.equals(review.getDestination().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review does not belong to this destination");
        }
        LocalDate visitDate = review.getVisitDate();
        if (visitDate != null && visitDate.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot verify a review for a future visit date");
        }
        UserDTO user = fetchUserForReviewVerification(request.getVerifiedBy());
        if (user.role() == null || !"ADMIN".equalsIgnoreCase(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an ADMIN user may verify reviews");
        }
        review.setVerified(true);
        Map<String, Object> metadata = review.getMetadata() == null ? new HashMap<>() : new HashMap<>(review.getMetadata());
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
        String cacheKey = "destination-service::S2-F9::" + maxRating;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                List<DestinationReviewAlertDTO> list = (List<DestinationReviewAlertDTO>) cached;
                return list;
            }
        } catch (Exception e) {
            logger.warn("Redis read failed", e);
        }

        List<DestinationReview> reviews = destinationReviewRepository.findLowRatedReviewsWithDestination(maxRating);
        if (reviews.isEmpty()) {
            return List.of();
        }
        Map<Long, List<DestinationReview>> byDestination = new LinkedHashMap<>();
        for (DestinationReview review : reviews) {
            Destination dest = review.getDestination();
            if (dest == null || dest.getId() == null) continue;
            byDestination.computeIfAbsent(dest.getId(), _id -> new ArrayList<>()).add(review);
        }
        List<DestinationReviewAlertDTO> result = new ArrayList<>(byDestination.size());
        for (List<DestinationReview> group : byDestination.values()) {
            Destination destination = group.get(0).getDestination();
            result.add(DestinationReviewAlertDTO.builder()
                    .destinationId(destination.getId())
                    .destinationName(destination.getName())
                    .destinationStatus(destination.getStatus().name())
                    .lowRatedReviews(group)
                    .lowRatedCount(group.size())
                    .build());
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, result, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Redis write failed", e);
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
        String cacheKey = "destination-service::S2-F1::" + category + "::" + minRating + "::" + maxRating;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                List<Destination> list = (List<Destination>) cached;
                return list;
            }
        } catch (Exception e) {
            logger.warn("Redis read failed", e);
        }
        List<Destination> result = destinationRepository.searchByCategoryAndRatingRange(category, minRating, maxRating);
        try {
            redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Redis write failed", e);
        }
        return result;
    }

    // ─── M2 Features ─────────────────────────────────────────────────────────

    /**
     * S2-F10: Full-text destination search via Elasticsearch.
     */
    @Transactional(readOnly = true)
    public List<DestinationSearchResultDTO> fullTextSearch(String query, String category,
                                                           String status, Double minRating, Double maxRating) {
        String cacheKey = "destination-service::S2-F10::" + Objects.hash(query, category, status, minRating, maxRating);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                List<DestinationSearchResultDTO> list = (List<DestinationSearchResultDTO>) cached;
                return list;
            }
        } catch (Exception e) {
            logger.warn("Redis read failed for S2-F10", e);
        }

        List<DestinationSearchResultDTO> result;
        try {
            Criteria criteria = new Criteria();
            if (query != null && !query.isBlank()) {
                criteria = criteria.and(new Criteria("name").contains(query))
                        .or(new Criteria("description").contains(query))
                        .or(new Criteria("highlights").contains(query));
            }
            if (category != null && !category.isBlank()) {
                criteria = criteria.and(new Criteria("category").is(category.toUpperCase()));
            }
            if (status != null && !status.isBlank()) {
                criteria = criteria.and(new Criteria("status").is(status.toUpperCase()));
            }
            if (minRating != null) {
                criteria = criteria.and(new Criteria("rating").greaterThanEqual(minRating));
            }
            if (maxRating != null) {
                criteria = criteria.and(new Criteria("rating").lessThanEqual(maxRating));
            }

            CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
            SearchHits<DestinationSearchDocument> hits = elasticsearchOperations.search(
                    criteriaQuery, DestinationSearchDocument.class);

            result = hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(elasticsearchHitAdapter::adapt)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Elasticsearch query failed, returning empty list", e);
            result = List.of();
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Redis write failed", e);
        }
        return result;
    }

    /**
     * S2-F11: Explicitly index a destination into Elasticsearch.
     */
    @Transactional(readOnly = true)
    public void indexDestination(Long destinationId) {
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        elasticsearchIndexService.indexDestination(destination, "explicit");
        cacheInvalidationService.evictFullTextSearchCaches();

        // Build list of indexed fields for the event details
        List<String> indexedFields = List.of("id", "name", "country", "category",
                "description", "highlights", "rating", "totalRatings", "status");

        Map<String, Object> payload = destinationPayload(destination);
        payload.put("indexedFields", indexedFields);
        payload.put("source", "explicit");
        notifyObservers("INDEXED", payload);
    }


    /**
     * S2-F12: Get Destination Analytics Dashboard.
     *
     * M3 change: the itinerary aggregation (totalItineraries, completedItineraries,
     * totalVisitors) is obtained via a single Feign call to itinerary-service at
     * GET /api/itineraries/destination/{destinationId}/dashboard-aggregate
     * instead of querying the shared database directly.
     *
     * Observer contract (M2 preserved): DASHBOARD_VIEWED is fired on every
     * invocation, including cache hits. The observer notify happens before the
     * cache check so it can never be skipped.
     *
     * Cache: 10-minute TTL, keyed by destinationId.
     */
    public DestinationDashboardDTO getDestinationDashboard(Long destinationId) {
        // 1. Validate destination exists — 404 if not found
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        // 2. Always fire DASHBOARD_VIEWED regardless of cache state
        Map<String, Object> eventPayload = destinationPayload(destination);
        eventPayload.put("dashboardParams", Map.of("destinationId", destinationId));
        notifyObservers("DASHBOARD_VIEWED", eventPayload);

        // 3. Return cached response if present
        String cacheKey = "destination-service::S2-F12::" + destinationId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof DestinationDashboardDTO hit) {
                return hit;
            }
        } catch (Exception e) {
            logger.warn("Redis read failed for S2-F12 key {}", cacheKey, e);
        }

        // 4. Fetch itinerary aggregates from itinerary-service via Feign (M3)
        DestinationDashboardAggregateDTO aggregate =
                itineraryServiceClient.getDestinationDashboardAggregate(destinationId);

        long totalItineraries     = aggregate != null && aggregate.totalItineraries()     != null ? aggregate.totalItineraries()     : 0L;
        long completedItineraries = aggregate != null && aggregate.completedItineraries() != null ? aggregate.completedItineraries() : 0L;
        long totalVisitors        = aggregate != null && aggregate.totalVisitors()        != null ? aggregate.totalVisitors()        : 0L;

        // 5. Build response — ratings come from the local Destination row (unchanged from M2)
        DestinationDashboardDTO dto = DestinationDashboardDTO.builder()
                .destinationId(destination.getId())
                .name(destination.getName())
                .totalItineraries(totalItineraries)
                .completedItineraries(completedItineraries)
                .totalVisitors(totalVisitors)
                .totalRatings(destination.getTotalRatings() != null ? destination.getTotalRatings() : 0)
                .averageRating(destination.getRating() != null ? destination.getRating() : 0.0)
                .build();

        // 6. Cache for 10 minutes
        try {
            redisTemplate.opsForValue().set(cacheKey, dto, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Redis write failed for S2-F12 key {}", cacheKey, e);
        }

        return dto;
    }

    private ItineraryDTO fetchItinerary(Long itineraryId) {
        try {
            return itineraryServiceClient.getItinerary(itineraryId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }
    }

    private UserDTO fetchUserForReviewVerification(Long userId) {
        try {
            return userServiceClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an ADMIN user may verify reviews");
        }
    }
}