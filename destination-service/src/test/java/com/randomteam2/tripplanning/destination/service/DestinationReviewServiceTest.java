package com.randomteam2.tripplanning.destination.service;

import com.randomteam2.tripplanning.destination.dto.DestinationReviewUpdateRequest;
import com.randomteam2.tripplanning.destination.exception.ResourceNotFoundException;
import com.randomteam2.tripplanning.destination.model.Destination;
import com.randomteam2.tripplanning.destination.model.DestinationReview;
import com.randomteam2.tripplanning.destination.model.DestinationReviewType;
import com.randomteam2.tripplanning.destination.observer.EntityObserver;
import com.randomteam2.tripplanning.destination.repository.DestinationRepository;
import com.randomteam2.tripplanning.destination.repository.DestinationReviewRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DestinationReviewServiceTest {

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private DestinationReviewRepository destinationReviewRepository;

    @Mock
    private DestinationCacheInvalidationService cacheInvalidationService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private EntityObserver observer;

    @Mock
    private ValueOperations<String, Object> valueOps;

    private DestinationReviewService service;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new DestinationReviewService(
                destinationRepository,
                destinationReviewRepository,
                cacheInvalidationService,
                redisTemplate,
                validator,
                List.of(observer));
    }

    @Test
    void createReview_destinationMissing_throwsResourceNotFound() {
        when(destinationRepository.findById(9L)).thenReturn(Optional.empty());

        DestinationReview review = validReview(null);

        assertThatThrownBy(() -> service.createReview(9L, review))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Destination not found");

        verify(destinationReviewRepository, never()).save(any());
    }

    @Test
    void createReview_persistsAndNotifies() {
        Destination dest = new Destination();
        dest.setId(1L);
        dest.setName("Paris");
        dest.setStatus(Destination.Status.ACTIVE);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(destinationReviewRepository.save(any(DestinationReview.class))).thenAnswer(i -> i.getArgument(0));

        DestinationReview review = validReview(null);
        DestinationReview saved = service.createReview(1L, review);

        assertThat(saved.getDestination()).isEqualTo(dest);
        verify(destinationReviewRepository).save(review);
        verify(cacheInvalidationService).evictDestinationCaches(1L);
        verify(observer).onEvent(eq("REVIEW_CREATED"), any());
    }

    @Test
    void getReviewById_found_returnsFromRepository() {
        DestinationReview r = validReview(5L);
        when(valueOps.get(anyString())).thenReturn(null);
        when(destinationReviewRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThat(service.getReviewById(5L)).isSameAs(r);
        verify(destinationReviewRepository).findById(5L);
    }

    @Test
    void getReviewById_missing_throws() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(destinationReviewRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReviewById(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review not found");
    }

    @Test
    void getReviewsByDestination_destinationMissing_throws() {
        when(destinationRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.getReviewsByDestination(2L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(destinationReviewRepository, never()).findByDestination_Id(any());
    }

    @Test
    void getReviewsByDestination_returnsList() {
        when(destinationRepository.existsById(1L)).thenReturn(true);
        DestinationReview r = validReview(1L);
        when(destinationReviewRepository.findByDestination_Id(1L)).thenReturn(List.of(r));

        assertThat(service.getReviewsByDestination(1L)).containsExactly(r);
    }

    @Test
    void updateReview_patchRating_updates() {
        Destination dest = new Destination();
        dest.setId(1L);
        DestinationReview existing = validReview(10L);
        existing.setDestination(dest);
        existing.setRating(3);
        when(destinationReviewRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(destinationReviewRepository.save(any(DestinationReview.class))).thenAnswer(i -> i.getArgument(0));

        DestinationReviewUpdateRequest patch = new DestinationReviewUpdateRequest();
        patch.setRating(5);

        DestinationReview out = service.updateReview(10L, patch);

        assertThat(out.getRating()).isEqualTo(5);
        verify(cacheInvalidationService).evictDestinationReviewCaches(1L, 10L);
    }

    @Test
    void deleteReview_removesAndEvictsCache() {
        Destination dest = new Destination();
        dest.setId(2L);
        DestinationReview existing = validReview(7L);
        existing.setDestination(dest);
        when(destinationReviewRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(destinationRepository.findById(2L)).thenReturn(Optional.of(dest));

        service.deleteReview(7L);

        verify(destinationReviewRepository).deleteById(7L);
        verify(cacheInvalidationService).evictDestinationReviewCaches(2L, 7L);
        verify(observer).onEvent(eq("REVIEW_DELETED"), any());
    }

    private static DestinationReview validReview(Long id) {
        DestinationReview r = new DestinationReview();
        r.setId(id);
        r.setType(DestinationReviewType.VISITOR);
        r.setContent("Nice place");
        r.setRating(4);
        r.setVisitDate(LocalDate.of(2026, 1, 1));
        r.setVerified(false);
        return r;
    }
}
