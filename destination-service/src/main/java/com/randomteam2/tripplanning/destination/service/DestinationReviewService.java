package com.randomteam2.tripplanning.destination.service;

import com.randomteam2.tripplanning.destination.dto.DestinationReviewUpdateRequest;
import com.randomteam2.tripplanning.destination.exception.ResourceNotFoundException;
import com.randomteam2.tripplanning.destination.model.Destination;
import com.randomteam2.tripplanning.destination.model.DestinationReview;
import com.randomteam2.tripplanning.destination.observer.EntityObserver;
import com.randomteam2.tripplanning.destination.repository.DestinationRepository;
import com.randomteam2.tripplanning.destination.repository.DestinationReviewRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class DestinationReviewService {

    private static final Logger logger = LoggerFactory.getLogger(DestinationReviewService.class);

    private final DestinationRepository destinationRepository;
    private final DestinationReviewRepository destinationReviewRepository;
    private final DestinationCacheInvalidationService cacheInvalidationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Validator validator;
    private final List<EntityObserver> observers;

    public DestinationReviewService(
            DestinationRepository destinationRepository,
            DestinationReviewRepository destinationReviewRepository,
            DestinationCacheInvalidationService cacheInvalidationService,
            RedisTemplate<String, Object> redisTemplate,
            Validator validator,
            List<EntityObserver> observers) {
        this.destinationRepository = destinationRepository;
        this.destinationReviewRepository = destinationReviewRepository;
        this.cacheInvalidationService = cacheInvalidationService;
        this.redisTemplate = redisTemplate;
        this.validator = validator;
        this.observers = observers != null ? observers : List.of();
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

    private void validateReview(DestinationReview review) {
        Set<ConstraintViolation<DestinationReview>> violations = validator.validate(review);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    @Transactional
    public DestinationReview createReview(Long destinationId, DestinationReview review) {
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found: " + destinationId));
        review.setId(null);
        review.setDestination(destination);
        if (review.getVerified() == null) {
            review.setVerified(false);
        }
        validateReview(review);
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
    public DestinationReview getReviewById(Long id) {
        String cacheKey = "destination-service::destination-review::" + id;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof DestinationReview r) {
                return r;
            }
        } catch (Exception e) {
            logger.warn("Redis read failed for key {}", cacheKey, e);
        }
        DestinationReview review = destinationReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
        try {
            redisTemplate.opsForValue().set(cacheKey, review, 15, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Redis write failed for key {}", cacheKey, e);
        }
        return review;
    }

    @Transactional(readOnly = true)
    public List<DestinationReview> getReviewsByDestination(Long destinationId) {
        if (!destinationRepository.existsById(destinationId)) {
            throw new ResourceNotFoundException("Destination not found: " + destinationId);
        }
        return destinationReviewRepository.findByDestination_Id(destinationId);
    }

    @Transactional
    public DestinationReview updateReview(Long id, DestinationReview updatedReview) {
        DestinationReview existing = destinationReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
        if (updatedReview.getContent() != null) {
            existing.setContent(updatedReview.getContent());
        }
        if (updatedReview.getRating() != null) {
            existing.setRating(updatedReview.getRating());
        }
        if (updatedReview.getVisitDate() != null) {
            existing.setVisitDate(updatedReview.getVisitDate());
        }
        if (updatedReview.getType() != null) {
            existing.setType(updatedReview.getType());
        }
        if (updatedReview.getMetadata() != null) {
            existing.setMetadata(updatedReview.getMetadata());
        }
        if (updatedReview.getVerified() != null) {
            existing.setVerified(updatedReview.getVerified());
        }
        validateReview(existing);
        DestinationReview saved = destinationReviewRepository.save(existing);
        Long destId = existing.getDestination() != null ? existing.getDestination().getId() : null;
        cacheInvalidationService.evictDestinationReviewCaches(destId, id);
        return saved;
    }

    /**
     * Applies only non-null fields from the update request.
     */
    @Transactional
    public DestinationReview updateReview(Long id, DestinationReviewUpdateRequest patch) {
        DestinationReview patchEntity = new DestinationReview();
        if (patch.getContent() != null) {
            patchEntity.setContent(patch.getContent());
        }
        if (patch.getRating() != null) {
            patchEntity.setRating(patch.getRating());
        }
        if (patch.getVisitDate() != null) {
            patchEntity.setVisitDate(patch.getVisitDate());
        }
        if (patch.getType() != null) {
            patchEntity.setType(patch.getType());
        }
        if (patch.getMetadata() != null) {
            patchEntity.setMetadata(patch.getMetadata());
        }
        if (patch.getVerified() != null) {
            patchEntity.setVerified(patch.getVerified());
        }
        Set<ConstraintViolation<DestinationReviewUpdateRequest>> patchViolations = validator.validate(patch);
        if (!patchViolations.isEmpty()) {
            throw new ConstraintViolationException(patchViolations);
        }
        return updateReview(id, patchEntity);
    }

    @Transactional
    public void deleteReview(Long id) {
        DestinationReview review = destinationReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
        Long destId = review.getDestination() != null ? review.getDestination().getId() : null;
        destinationReviewRepository.deleteById(id);
        cacheInvalidationService.evictDestinationReviewCaches(destId, id);
        if (destId != null) {
            destinationRepository.findById(destId).ifPresent(d -> notifyObservers("REVIEW_DELETED", destinationPayload(d)));
        }
    }
}
