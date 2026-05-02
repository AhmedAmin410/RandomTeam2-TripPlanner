package com.randomteam2.tripplanning.destination.service;

import com.randomteam2.tripplanning.destination.adapter.ElasticsearchHitAdapter;
import com.randomteam2.tripplanning.destination.adapter.ObjectArrayDtoAdapter;
import com.randomteam2.tripplanning.destination.exception.InvalidRatingRangeException;
import com.randomteam2.tripplanning.destination.exception.InvalidSearchParameterException;
import com.randomteam2.tripplanning.destination.model.Destination;
import com.randomteam2.tripplanning.destination.observer.MongoEventLogger;
import com.randomteam2.tripplanning.destination.repository.DestinationEventRepository;
import com.randomteam2.tripplanning.destination.repository.DestinationRepository;
import com.randomteam2.tripplanning.destination.repository.DestinationReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DestinationServiceSearchBehaviorTest {

    @Mock
    private DestinationRepository destinationRepository;
    @Mock
    private DestinationReviewRepository destinationReviewRepository;
    @Mock
    private MongoEventLogger mongoEventLogger;
    @Mock
    private DestinationCacheInvalidationService cacheInvalidationService;
    @Mock
    private ElasticsearchIndexService elasticsearchIndexService;
    @Mock
    private ElasticsearchOperations elasticsearchOperations;
    @Mock
    private ElasticsearchHitAdapter elasticsearchHitAdapter;
    @Mock
    private ObjectArrayDtoAdapter objectArrayDtoAdapter;
    @Mock
    private DestinationEventRepository destinationEventRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOps;

    private DestinationService destinationService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        destinationService = new DestinationService(
                destinationRepository,
                destinationReviewRepository,
                mongoEventLogger,
                cacheInvalidationService,
                elasticsearchIndexService,
                elasticsearchOperations,
                elasticsearchHitAdapter,
                objectArrayDtoAdapter,
                destinationEventRepository,
                redisTemplate);
    }

    @Test
    void searchDestinations_minGreaterThanMax_throwsWithExactMessage() {
        assertThatThrownBy(() -> destinationService.searchDestinations(null, 5.0, 2.0))
                .isInstanceOf(InvalidRatingRangeException.class)
                .hasMessage("minRating cannot be greater than maxRating");
        verify(destinationRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void searchDestinations_invalidCategory_throws() {
        assertThatThrownBy(() -> destinationService.searchDestinations("UNKNOWN", null, null))
                .isInstanceOf(InvalidSearchParameterException.class)
                .hasMessageContaining("Invalid category");
        verify(destinationRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void searchDestinations_categoryOnly_callsRepository() {
        Destination beach = minimalDestination(1L, Destination.Category.BEACH, 3.0);
        when(destinationRepository.findAll(any(Specification.class))).thenReturn(List.of(beach));

        List<Destination> out = destinationService.searchDestinations("beach", null, null);

        assertThat(out).containsExactly(beach);
        verify(destinationRepository).findAll(any(Specification.class));
    }

    @Test
    void searchDestinations_minRatingOnly() {
        Destination d = minimalDestination(2L, Destination.Category.CITY, 4.2);
        when(destinationRepository.findAll(any(Specification.class))).thenReturn(List.of(d));

        assertThat(destinationService.searchDestinations(null, 4.0, null)).containsExactly(d);
    }

    @Test
    void searchDestinations_maxRatingOnly() {
        Destination d = minimalDestination(3L, Destination.Category.CITY, 2.0);
        when(destinationRepository.findAll(any(Specification.class))).thenReturn(List.of(d));

        assertThat(destinationService.searchDestinations(null, null, 3.0)).containsExactly(d);
    }

    @Test
    void searchDestinations_combinedFilters() {
        Destination d = minimalDestination(4L, Destination.Category.MOUNTAIN, 4.5);
        when(destinationRepository.findAll(any(Specification.class))).thenReturn(List.of(d));

        assertThat(destinationService.searchDestinations("MOUNTAIN", 4.0, 5.0)).containsExactly(d);
    }

    @Test
    void searchDestinations_noFilters_returnsAllSortedByRatingDesc() {
        Destination low = minimalDestination(1L, Destination.Category.BEACH, 2.0);
        Destination high = minimalDestination(2L, Destination.Category.CITY, 5.0);
        List<Destination> unsorted = new ArrayList<>(List.of(low, high));
        when(destinationRepository.findAll(any(Specification.class))).thenReturn(unsorted);

        List<Destination> out = destinationService.searchDestinations(null, null, null);

        assertThat(out).extracting(Destination::getRating).containsExactly(5.0, 2.0);
    }

    @Test
    void searchDestinations_oneRatingBound_noRangeValidationAgainstMissing() {
        when(destinationRepository.findAll(any(Specification.class))).thenReturn(List.of());

        destinationService.searchDestinations(null, 3.0, null);
        destinationService.searchDestinations(null, null, 4.0);

        verify(destinationRepository, times(2)).findAll(any(Specification.class));
    }

    private static Destination minimalDestination(long id, Destination.Category category, Double rating) {
        Destination d = new Destination();
        d.setId(id);
        d.setName("n" + id);
        d.setCountry("C");
        d.setDescription("D");
        d.setCategory(category);
        d.setStatus(Destination.Status.ACTIVE);
        d.setRating(rating);
        return d;
    }
}
