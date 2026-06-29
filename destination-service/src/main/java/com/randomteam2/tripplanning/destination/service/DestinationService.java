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
import com.randomteam2.tripplanning.destination.repository.DestinationEventRepository;
import com.randomteam2.tripplanning.destination.repository.DestinationRepository;
import com.randomteam2.tripplanning.destination.repository.DestinationReviewRepository;
import com.randmteam2.tripplanning.contracts.dto.DestinationBookingRevenueAggregateDTO;
import com.randmteam2.tripplanning.contracts.dto.DestinationDashboardAggregateDTO;
import com.randmteam2.tripplanning.contracts.dto.BatchDestinationRequest;
import com.randmteam2.tripplanning.contracts.dto.DestinationSummaryDTO;
import com.randmteam2.tripplanning.contracts.feign.ItineraryServiceClient;
import com.randmteam2.tripplanning.contracts.feign.UserServiceClient;
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
            UserServiceClient userServiceClient) {
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
    public List<DestinationSummaryDTO> batchGetDestinations(BatchDestinationRequest request) {
        if (request == null || request.destinationIds() == null) {
            return List.of();
        }
        return destinationRepository.findAllById(request.destinationIds()).stream()
                .map(d -> new DestinationSummaryDTO(
                        d.getId(),
                        d.getName(),
                        d.getCountry(),
                        d.getCategory() != null ? d.getCategory().name() : null))
                .toList();
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

    @Transactional(readOnly = true)
    public DestinationRevenueDTO getDestinationRevenueSummary(Long destinationId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must not be before startDate");
        }

        String cacheKey = "destination-service::S2-F3::" + destinationId + "::" + startDate + "::" + endDate;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof DestinationRevenueDTO dto) {
                return dto;
            }
        } catch (Exception e) {
            logger.warn("Redis read failed", e);
        }

        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        Object[] row = legacyDestinationRevenueSummary(destinationId, startDate, endDate);
        if (row == null) {
            DestinationBookingRevenueAggregateDTO aggregate = itineraryServiceClient.getDestinationBookingRevenue(
                    destinationId, startDate.toString(), endDate.toString());
            if (aggregate != null) {
                row = new Object[]{
                        aggregate.totalBookings(),
                        aggregate.totalRevenue(),
                        aggregate.averageBookingAmount()
                };
            }
        }
        if (row != null && row.length == 1 && row[0] instanceof Object[]) {
            row = (Object[]) row[0];
        }

        // Adapter pattern: convert Object[] → DTO
        DestinationRevenueDTO dto = objectArrayDtoAdapter.adaptRevenue(destination.getId(), destination.getName(), row);

        try {
            redisTemplate.opsForValue().set(cacheKey, dto, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Redis write failed", e);
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
            long activeRefs = Math.max(
                    legacyActiveItineraryReferences(id),
                    itineraryServiceClient.getDestinationActiveItineraryCount(id));
            if (activeRefs > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active itineraries still reference this destination");
            }
        }
        Destination.Status oldStatus = destination.getStatus();
        destination.setStatus(newStatus);
        Destination savedDestination = destinationRepository.save(destination);
        elasticsearchIndexService.indexDestination(savedDestination, "auto_crud_update");
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

        List<Object[]> legacyRows = legacyTopRatedDestinationsReport(limit);
        if (legacyRows != null) {
            List<TopDestinationDTO> result = new ArrayList<>(legacyRows.size());
            for (Object[] row : legacyRows) {
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

        List<Destination> destinations = destinationRepository.findAll().stream()
                .sorted(Comparator.comparing(
                                (Destination d) -> d.getRating() != null ? d.getRating() : 0.0)
                        .reversed()
                        .thenComparing(Destination::getId))
                .limit(limit)
                .toList();
        List<TopDestinationDTO> result = new ArrayList<>(destinations.size());
        for (Destination dest : destinations) {
            DestinationBookingRevenueAggregateDTO aggregate = itineraryServiceClient.getDestinationBookingRevenue(
                    dest.getId(), "1900-01-01", "2100-01-01");
            result.add(TopDestinationDTO.builder()
                    .destinationId(dest.getId())
                    .name(dest.getName())
                    .rating(dest.getRating() != null ? dest.getRating() : 0.0)
                    .totalBookings(nullToZero(aggregate.totalBookings()))
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
        List<Object[]> legacyItineraryRows = legacyItineraryDestinationIdAndStatus(request.getItineraryId());
        Long itineraryDestinationId;
        String status;
        if (legacyItineraryRows != null && legacyItineraryRows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }
        if (legacyItineraryRows != null) {
            Object[] row = legacyItineraryRows.get(0);
            itineraryDestinationId = row[0] != null ? ((Number) row[0]).longValue() : null;
            status = row[1] != null ? row[1].toString() : null;
        } else {
            Map<String, Object> itinerary = getItineraryMap(request.getItineraryId());
            itineraryDestinationId = asLong(itinerary.get("destinationId"));
            status = itinerary.get("status") != null ? itinerary.get("status").toString() : null;
        }
        if (itineraryDestinationId == null || !itineraryDestinationId.equals(destinationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Itinerary does not reference this destination");
        }
        if (!"COMPLETED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Itinerary must be COMPLETED to rate this destination");
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
        return savedDestination;
    }

    @Transactional
    public Destination verifyDestinationReview(Long destinationId, Long reviewId, VerifyDestinationReviewRequest request) {
        destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
        if (request == null || request.getVerifiedBy() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verifiedBy is required");
        }
        Long legacyAdminCount = legacyAdminUserCount(request.getVerifiedBy());
        boolean isAdmin = legacyAdminCount != null
                ? legacyAdminCount > 0
                : "ADMIN".equals(String.valueOf(getUserMap(request.getVerifiedBy()).get("role")));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an ADMIN user may verify reviews");
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
     * Logs DASHBOARD_VIEWED on every invocation (even cache hits) – logging is outside cache.
     */
    public DestinationDashboardDTO getDestinationDashboard(Long destinationId) {
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        // Always log DASHBOARD_VIEWED (outside cache check)
        Map<String, Object> viewPayload = destinationPayload(destination);
        viewPayload.put("dashboardParams", Map.of("destinationId", destinationId));
        notifyObservers("DASHBOARD_VIEWED", viewPayload);

        String cacheKey = "destination-service::S2-F12::" + destinationId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof DestinationDashboardDTO dto) {
                return dto;
            }
        } catch (Exception e) {
            logger.warn("Redis read failed for S2-F12", e);
        }

        Object[] stats = legacyDestinationDashboardStats(destinationId);
        long totalItineraries;
        long completedItineraries;
        long totalVisitors;
        if (stats != null) {
            if (stats.length == 1 && stats[0] instanceof Object[]) {
                stats = (Object[]) stats[0];
            }
            totalItineraries = stats[0] != null ? ((Number) stats[0]).longValue() : 0L;
            completedItineraries = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
            totalVisitors = stats[2] != null ? ((Number) stats[2]).longValue() : 0L;
        } else {
            DestinationDashboardAggregateDTO aggregate = itineraryServiceClient.getDestinationDashboardAggregate(destinationId);
            totalItineraries = aggregate != null ? nullToZero(aggregate.totalItineraries()) : 0L;
            completedItineraries = aggregate != null ? nullToZero(aggregate.completedItineraries()) : 0L;
            totalVisitors = aggregate != null ? nullToZero(aggregate.totalVisitors()) : 0L;
        }

        DestinationDashboardDTO dto = DestinationDashboardDTO.builder()
                .destinationId(destination.getId())
                .name(destination.getName())
                .totalItineraries(totalItineraries)
                .completedItineraries(completedItineraries)
                .totalVisitors(totalVisitors)
                .totalRatings(destination.getTotalRatings() != null ? destination.getTotalRatings() : 0)
                .averageRating(destination.getRating() != null ? destination.getRating() : 0.0)
                .build();

        try {
            redisTemplate.opsForValue().set(cacheKey, dto, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Redis write failed for S2-F12", e);
        }
        return dto;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getItineraryMap(Long itineraryId) {
        try {
            Object response = itineraryServiceClient.getItinerary(itineraryId);
            if (response instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid itinerary response");
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found", e);
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to validate itinerary", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getUserMap(Long userId) {
        try {
            Object response = userServiceClient.getUser(userId);
            if (response instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid user response");
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found", e);
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to validate user", e);
        }
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return null;
        return Long.valueOf(value.toString());
    }

    private long nullToZero(Long value) {
        return value != null ? value : 0L;
    }

    private double nullToZero(Double value) {
        return value != null ? value : 0.0;
    }

    private Object[] legacyDestinationRevenueSummary(Long destinationId, LocalDate startDate, LocalDate endDate) {
        try {
            return destinationRepository.findDestinationRevenueSummary(destinationId, startDate, endDate);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private long legacyActiveItineraryReferences(Long destinationId) {
        try {
            return destinationRepository.countActiveItinerariesReferencingDestination(destinationId);
        } catch (UnsupportedOperationException e) {
            return 0L;
        }
    }

    private List<Object[]> legacyTopRatedDestinationsReport(int limit) {
        try {
            return destinationRepository.findTopRatedDestinationsReport(limit);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private List<Object[]> legacyItineraryDestinationIdAndStatus(Long itineraryId) {
        try {
            return destinationRepository.findItineraryDestinationIdAndStatus(itineraryId);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private Long legacyAdminUserCount(Long userId) {
        try {
            return destinationRepository.countAdminUserById(userId);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private Object[] legacyDestinationDashboardStats(Long destinationId) {
        try {
            return destinationRepository.findDestinationDashboardStats(destinationId);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
