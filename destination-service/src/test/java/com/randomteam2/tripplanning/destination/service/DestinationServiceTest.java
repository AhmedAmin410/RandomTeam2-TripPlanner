package com.randomteam2.tripplanning.destination.service;

import com.randomteam2.tripplanning.destination.adapter.ElasticsearchHitAdapter;
import com.randomteam2.tripplanning.destination.adapter.MongoDocumentAdapter;
import com.randomteam2.tripplanning.destination.adapter.ObjectArrayDtoAdapter;
import com.randomteam2.tripplanning.destination.dto.DestinationBookingRevenueAggregateDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationBatchRequest;
import com.randomteam2.tripplanning.destination.dto.DestinationDashboardAggregateDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationDashboardDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationRateRequest;
import com.randomteam2.tripplanning.destination.dto.DestinationReviewAlertDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationRevenueDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationSummaryDTO;
import com.randomteam2.tripplanning.destination.dto.ItineraryDTO;
import com.randomteam2.tripplanning.destination.dto.StatusChangedEvent;
import com.randomteam2.tripplanning.destination.dto.UserDTO;
import com.randomteam2.tripplanning.destination.feign.ItineraryServiceClient;
import com.randomteam2.tripplanning.destination.feign.UserServiceClient;
import com.randomteam2.tripplanning.destination.messaging.DestinationEventPublisher;
import com.randomteam2.tripplanning.destination.dto.DestinationSearchResultDTO;
import com.randomteam2.tripplanning.destination.dto.TopDestinationDTO;
import com.randomteam2.tripplanning.destination.dto.VerifyDestinationReviewRequest;
import com.randomteam2.tripplanning.destination.elasticsearch.DestinationSearchDocument;
import com.randomteam2.tripplanning.destination.elasticsearch.DestinationSearchRepository;
import com.randomteam2.tripplanning.destination.mongo.DestinationEvent;
import com.randomteam2.tripplanning.destination.mongo.EventFactory;
import com.randomteam2.tripplanning.destination.mongo.EventType;
import com.randomteam2.tripplanning.destination.mongo.MongoEvent;
import com.randomteam2.tripplanning.destination.model.Destination;
import com.randomteam2.tripplanning.destination.model.DestinationReview;
import com.randomteam2.tripplanning.destination.model.ReviewType;
import com.randomteam2.tripplanning.destination.observer.EntityObserver;
import com.randomteam2.tripplanning.destination.observer.MongoEventLogger;
import com.randomteam2.tripplanning.destination.repository.DestinationEventRepository;
import com.randomteam2.tripplanning.destination.repository.DestinationRepository;
import com.randomteam2.tripplanning.destination.repository.DestinationReviewRepository;
import com.randomteam2.tripplanning.destination.security.AuthHandler;
import com.randomteam2.tripplanning.destination.security.AuthContext;
import com.randomteam2.tripplanning.destination.security.JwtConfigurationManager;
import com.randomteam2.tripplanning.destination.security.RoleAuthorizationHandler;
import com.randomteam2.tripplanning.destination.security.SignatureValidationHandler;
import com.randomteam2.tripplanning.destination.security.TokenExtractionHandler;
import com.randomteam2.tripplanning.destination.security.UserLoaderHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for DestinationService covering all M3 spec features:
 * S2-F3, S2-F4, S2-F7, S2-F8, S2-F12 (Feign-based),
 * S2-F1, S2-F2, S2-F5, S2-F6, S2-F9, S2-F10, S2-F11 (local),
 * batch endpoint, CRUD events, Observer/Builder/Chain/Singleton/Factory/Adapter patterns.
 */
@ExtendWith(MockitoExtension.class)
class DestinationServiceTest {

    @Mock private DestinationRepository destinationRepository;
    @Mock private DestinationReviewRepository destinationReviewRepository;
    @Mock private MongoEventLogger mongoEventLogger;
    @Mock private DestinationCacheInvalidationService cacheInvalidationService;
    @Mock private ElasticsearchIndexService elasticsearchIndexService;
    @Mock private ElasticsearchOperations elasticsearchOperations;
    @Mock private ElasticsearchHitAdapter elasticsearchHitAdapter;
    @Mock private ObjectArrayDtoAdapter objectArrayDtoAdapter;
    @Mock private DestinationEventRepository destinationEventRepository;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private ItineraryServiceClient itineraryServiceClient;
    @Mock private UserServiceClient userServiceClient;
    @Mock private DestinationEventPublisher destinationEventPublisher;
    @Mock private EntityObserver mockObserver;
    @InjectMocks private DestinationService destinationService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // =========================================================================
    // POST /api/destinations/batch — New endpoint required by S5-F10
    // =========================================================================

    @Test
    void getDestinationsBatch_returnsMatchingSummaries() {
        Destination d1 = newDestination(1L);
        d1.setName("Dahab");
        d1.setCountry("Egypt");
        d1.setCategory(Destination.Category.ADVENTURE);

        Destination d2 = newDestination(2L);
        d2.setName("Luxor");
        d2.setCountry("Egypt");
        d2.setCategory(Destination.Category.HISTORICAL);

        when(destinationRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(d1, d2));

        List<DestinationSummaryDTO> result = destinationService.getDestinationsBatch(
                new DestinationBatchRequest(List.of(1L, 2L)));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).destinationId()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("Dahab");
        assertThat(result.get(0).country()).isEqualTo("Egypt");
        assertThat(result.get(0).category()).isEqualTo("ADVENTURE");
        assertThat(result.get(1).destinationId()).isEqualTo(2L);
        assertThat(result.get(1).name()).isEqualTo("Luxor");
        assertThat(result.get(1).category()).isEqualTo("HISTORICAL");
    }

    @Test
    void getDestinationsBatch_nullRequest_returnsEmpty() {
        List<DestinationSummaryDTO> result = destinationService.getDestinationsBatch(null);
        assertThat(result).isEmpty();
        verify(destinationRepository, never()).findAllById(any());
    }

    @Test
    void getDestinationsBatch_emptyIds_returnsEmpty() {
        List<DestinationSummaryDTO> result = destinationService.getDestinationsBatch(
                new DestinationBatchRequest(List.of()));
        assertThat(result).isEmpty();
        verify(destinationRepository, never()).findAllById(any());
    }

    @Test
    void getDestinationsBatch_someIdsNotFound_returnsOnlyFound() {
        Destination d1 = newDestination(1L);
        d1.setName("Dahab");
        d1.setCountry("Egypt");
        d1.setCategory(Destination.Category.ADVENTURE);

        // ID 99 does not exist
        when(destinationRepository.findAllById(List.of(1L, 99L))).thenReturn(List.of(d1));

        List<DestinationSummaryDTO> result = destinationService.getDestinationsBatch(
                new DestinationBatchRequest(List.of(1L, 99L)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).destinationId()).isEqualTo(1L);
    }

    // =========================================================================
    // GET /api/destinations/{id} — DestinationDTO contract (S3 & S5 Feign)
    // =========================================================================

    @Test
    void getDestinationDTOById_returnsAllRequiredFields() {
        Destination dest = newDestination(1L);
        dest.setName("Dahab");
        dest.setCountry("Egypt");
        dest.setCategory(Destination.Category.ADVENTURE);
        dest.setStatus(Destination.Status.ACTIVE);
        dest.setRating(4.7);
        dest.setTotalRatings(10);
        dest.setDetails(Map.of("climate", "tropical"));

        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));

        DestinationDTO dto = destinationService.getDestinationDTOById(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Dahab");
        assertThat(dto.country()).isEqualTo("Egypt");
        assertThat(dto.category()).isEqualTo("ADVENTURE");
        assertThat(dto.status()).isEqualTo("ACTIVE");
        assertThat(dto.rating()).isEqualTo(4.7);
        assertThat(dto.totalRatings()).isEqualTo(10);
        assertThat(dto.details()).containsEntry("climate", "tropical");
    }

    @Test
    void getDestinationDTOById_notFound_throws404() {
        when(destinationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> destinationService.getDestinationDTOById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    // =========================================================================
    // S2-F3: Revenue Summary (GET /api/destinations/{id}/revenue)
    // M3: Single Feign call to itinerary-service replaces 3-table JOIN
    // =========================================================================

    @Test
    void revenueSummary_mapsAggregateValuesToDto() {
        Destination destination = newDestination(1L);
        destination.setName("Cairo");
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        DestinationBookingRevenueAggregateDTO aggregate = new DestinationBookingRevenueAggregateDTO(
                5L, BigDecimal.valueOf(2000.0), BigDecimal.valueOf(400.0));
        when(itineraryServiceClient.getDestinationBookingRevenue(1L, "2026-03-01", "2026-03-31"))
                .thenReturn(aggregate);
        when(objectArrayDtoAdapter.adaptRevenue(1L, "Cairo", aggregate))
                .thenReturn(DestinationRevenueDTO.builder()
                        .destinationId(1L).name("Cairo")
                        .totalBookings(5L).totalRevenue(2000.0).averageBookingAmount(400.0)
                        .build());
        DestinationRevenueDTO dto = destinationService.getDestinationRevenueSummary(1L, start, end);
        assertThat(dto.getDestinationId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Cairo");
        assertThat(dto.getTotalBookings()).isEqualTo(5L);
        assertThat(dto.getTotalRevenue()).isEqualTo(2000.0);
        assertThat(dto.getAverageBookingAmount()).isEqualTo(400.0);
        verify(destinationRepository).findById(1L);
        verify(itineraryServiceClient).getDestinationBookingRevenue(1L, "2026-03-01", "2026-03-31");
    }

    @Test
    void revenueSummary_noBookings_returnsZeroes() {
        Destination destination = newDestination(2L);
        destination.setName("Alexandria");
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);
        when(destinationRepository.findById(2L)).thenReturn(Optional.of(destination));
        DestinationBookingRevenueAggregateDTO aggregate = new DestinationBookingRevenueAggregateDTO(
                0L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(itineraryServiceClient.getDestinationBookingRevenue(2L, "2026-04-01", "2026-04-30"))
                .thenReturn(aggregate);
        when(objectArrayDtoAdapter.adaptRevenue(2L, "Alexandria", aggregate))
                .thenReturn(DestinationRevenueDTO.builder()
                        .destinationId(2L).name("Alexandria")
                        .totalBookings(0L).totalRevenue(0.0).averageBookingAmount(0.0)
                        .build());
        DestinationRevenueDTO dto = destinationService.getDestinationRevenueSummary(2L, start, end);
        assertThat(dto.getTotalBookings()).isEqualTo(0L);
        assertThat(dto.getTotalRevenue()).isEqualTo(0.0);
        assertThat(dto.getAverageBookingAmount()).isEqualTo(0.0);
    }

    @Test
    void revenueSummary_destinationNotFound_throws404() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        when(destinationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> destinationService.getDestinationRevenueSummary(99L, start, end))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
        // Feign must never be called if destination does not exist
        verify(itineraryServiceClient, never()).getDestinationBookingRevenue(any(), any(), any());
    }

    @Test
    void revenueSummary_endDateBeforeStartDate_throws400() {
        LocalDate start = LocalDate.of(2026, 3, 31);
        LocalDate end = LocalDate.of(2026, 3, 1);
        assertThatThrownBy(() -> destinationService.getDestinationRevenueSummary(1L, start, end))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        // date guard fires before any DB or Feign call
        verify(destinationRepository, never()).findById(any());
        verify(itineraryServiceClient, never()).getDestinationBookingRevenue(any(), any(), any());
    }

    @Test
    void revenueSummary_nullRow_returnsZeroes() {
        Destination destination = newDestination(1L);
        destination.setName("Cairo");
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(itineraryServiceClient.getDestinationBookingRevenue(1L, "2026-03-01", "2026-03-31")).thenReturn(null);
        when(objectArrayDtoAdapter.adaptRevenue(1L, "Cairo", (DestinationBookingRevenueAggregateDTO) null))
                .thenReturn(DestinationRevenueDTO.builder()
                        .destinationId(1L).name("Cairo")
                        .totalBookings(0L).totalRevenue(0.0).averageBookingAmount(0.0)
                        .build());
        DestinationRevenueDTO dto = destinationService.getDestinationRevenueSummary(1L, start, end);
        assertThat(dto.getTotalBookings()).isEqualTo(0L);
        assertThat(dto.getTotalRevenue()).isEqualTo(0.0);
        assertThat(dto.getAverageBookingAmount()).isEqualTo(0.0);
    }

    /**
     * Spec scenario (S2-F3):
     * Destination ID=1 ("Dahab", category=ADVENTURE) in destination-postgres.
     * 3 itineraries referencing destinationId=1 in itinerary-postgres.
     * 5 CONFIRMED bookings with amounts 200+300+400+500+600 = 2000 in March 2026.
     * Expects: totalBookings=5, totalRevenue=2000.00, averageBookingAmount=400.00.
     * Verifies: exactly ONE Feign call to itinerary-service; no direct JDBC to itinerary/booking DBs.
     */
    @Test
    void revenueSummary_specScenario_dahab5Bookings_returns2000Revenue() {
        Destination destination = newDestination(1L);
        destination.setName("Dahab");
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end   = LocalDate.of(2026, 3, 31);

        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));

        DestinationBookingRevenueAggregateDTO aggregate = new DestinationBookingRevenueAggregateDTO(
                5L, new BigDecimal("2000.00"), new BigDecimal("400.00"));
        when(itineraryServiceClient.getDestinationBookingRevenue(1L, "2026-03-01", "2026-03-31"))
                .thenReturn(aggregate);

        when(objectArrayDtoAdapter.adaptRevenue(1L, "Dahab", aggregate))
                .thenReturn(DestinationRevenueDTO.builder()
                        .destinationId(1L).name("Dahab")
                        .totalBookings(5L).totalRevenue(2000.00).averageBookingAmount(400.00)
                        .build());

        DestinationRevenueDTO dto = destinationService.getDestinationRevenueSummary(1L, start, end);

        assertThat(dto.getDestinationId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Dahab");
        assertThat(dto.getTotalBookings()).isEqualTo(5L);
        assertThat(dto.getTotalRevenue()).isEqualTo(2000.00);
        assertThat(dto.getAverageBookingAmount()).isEqualTo(400.00);

        // Exactly ONE Feign call — no direct cross-DB JDBC
        verify(itineraryServiceClient, times(1))
                .getDestinationBookingRevenue(1L, "2026-03-01", "2026-03-31");
        verify(destinationRepository, times(1)).findById(1L);
    }

    /**
     * Verifies LocalDate is serialised to "yyyy-MM-dd" strings before being passed to Feign.
     * Single-digit months/days must be zero-padded (2026-01-05 not 2026-1-5).
     */
    @Test
    void revenueSummary_dateParamsFormattedAsIso_yyyy_MM_dd() {
        Destination destination = newDestination(5L);
        destination.setName("Luxor");
        LocalDate start = LocalDate.of(2026, 1, 5);   // single-digit day & month
        LocalDate end   = LocalDate.of(2026, 12, 9);

        when(destinationRepository.findById(5L)).thenReturn(Optional.of(destination));

        DestinationBookingRevenueAggregateDTO aggregate =
                new DestinationBookingRevenueAggregateDTO(2L, BigDecimal.valueOf(800), BigDecimal.valueOf(400));
        when(itineraryServiceClient.getDestinationBookingRevenue(5L, "2026-01-05", "2026-12-09"))
                .thenReturn(aggregate);
        when(objectArrayDtoAdapter.adaptRevenue(5L, "Luxor", aggregate))
                .thenReturn(DestinationRevenueDTO.builder()
                        .destinationId(5L).name("Luxor")
                        .totalBookings(2L).totalRevenue(800.0).averageBookingAmount(400.0)
                        .build());

        DestinationRevenueDTO dto = destinationService.getDestinationRevenueSummary(5L, start, end);

        assertThat(dto.getTotalBookings()).isEqualTo(2L);
        // Critical assertion: exact ISO-8601 string format passed to Feign
        verify(itineraryServiceClient).getDestinationBookingRevenue(5L, "2026-01-05", "2026-12-09");
    }

    @Test
    void revenueSummary_nullStartDate_throws400() {
        assertThatThrownBy(() -> destinationService.getDestinationRevenueSummary(1L, null, LocalDate.of(2026, 3, 31)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).findById(any());
        verify(itineraryServiceClient, never()).getDestinationBookingRevenue(any(), any(), any());
    }

    @Test
    void revenueSummary_nullEndDate_throws400() {
        assertThatThrownBy(() -> destinationService.getDestinationRevenueSummary(1L, LocalDate.of(2026, 3, 1), null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).findById(any());
        verify(itineraryServiceClient, never()).getDestinationBookingRevenue(any(), any(), any());
    }

    @Test
    void revenueSummary_sameDayRange_isValid() {
        Destination destination = newDestination(3L);
        destination.setName("Sharm");
        LocalDate sameDay = LocalDate.of(2026, 6, 15);

        when(destinationRepository.findById(3L)).thenReturn(Optional.of(destination));

        DestinationBookingRevenueAggregateDTO aggregate =
                new DestinationBookingRevenueAggregateDTO(1L, BigDecimal.valueOf(500), BigDecimal.valueOf(500));
        when(itineraryServiceClient.getDestinationBookingRevenue(3L, "2026-06-15", "2026-06-15"))
                .thenReturn(aggregate);
        when(objectArrayDtoAdapter.adaptRevenue(3L, "Sharm", aggregate))
                .thenReturn(DestinationRevenueDTO.builder()
                        .destinationId(3L).name("Sharm")
                        .totalBookings(1L).totalRevenue(500.0).averageBookingAmount(500.0)
                        .build());

        DestinationRevenueDTO dto = destinationService.getDestinationRevenueSummary(3L, sameDay, sameDay);
        assertThat(dto.getTotalBookings()).isEqualTo(1L);
        verify(itineraryServiceClient, times(1)).getDestinationBookingRevenue(3L, "2026-06-15", "2026-06-15");
    }

    /**
     * Non-404 Feign failures must propagate — the service must NOT swallow them silently.
     */
    @Test
    void revenueSummary_feignServiceDown_propagatesException() {
        Destination destination = newDestination(1L);
        destination.setName("Dahab");
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end   = LocalDate.of(2026, 3, 31);

        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));

        feign.FeignException.ServiceUnavailable feignEx =
                new feign.FeignException.ServiceUnavailable(
                        "itinerary-service down",
                        feign.Request.create(feign.Request.HttpMethod.GET,
                                "/api/itineraries/destination/1/booking-revenue",
                                Map.of(), null, null, null),
                        null, null);
        when(itineraryServiceClient.getDestinationBookingRevenue(1L, "2026-03-01", "2026-03-31"))
                .thenThrow(feignEx);

        assertThatThrownBy(() -> destinationService.getDestinationRevenueSummary(1L, start, end))
                .isInstanceOf(Exception.class);
    }

    /**
     * Cache hit: second call with same params must NOT issue another Feign call.
     * Redis mock returns a cached DestinationRevenueDTO on the first get().
     */
    @Test
    void revenueSummary_cacheHit_skipsFeignCall() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end   = LocalDate.of(2026, 3, 31);
        String cacheKey = "destination-service::S2-F3::1::" + start + "::" + end;

        DestinationRevenueDTO cached = DestinationRevenueDTO.builder()
                .destinationId(1L).name("Dahab")
                .totalBookings(5L).totalRevenue(2000.0).averageBookingAmount(400.0)
                .build();

        when(valueOperations.get(cacheKey)).thenReturn(cached);

        DestinationRevenueDTO dto = destinationService.getDestinationRevenueSummary(1L, start, end);

        assertThat(dto.getTotalBookings()).isEqualTo(5L);
        assertThat(dto.getTotalRevenue()).isEqualTo(2000.0);
        // No Feign call and no DB call — served entirely from cache
        verify(itineraryServiceClient, never()).getDestinationBookingRevenue(any(), any(), any());
        verify(destinationRepository, never()).findById(any());
    }

    // =========================================================================
    // S2-F2: Update Destination Details (JSONB merge)
    // =========================================================================

    @Test
    void updateDetails_mergesIncomingFieldsWithExistingDetails() {
        Destination destination = newDestination(1L);
        Map<String, Object> existingDetails = new HashMap<>();
        existingDetails.put("climate", "tropical");
        existingDetails.put("currency", "USD");
        existingDetails.put("visaRequired", true);
        destination.setDetails(existingDetails);
        Map<String, Object> incomingDetails = new HashMap<>();
        incomingDetails.put("currency", "EUR");
        incomingDetails.put("timezone", "GMT+2");
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));
        Destination updated = destinationService.updateDetails(1L, incomingDetails);
        assertThat(updated.getDetails()).containsEntry("climate", "tropical");
        assertThat(updated.getDetails()).containsEntry("currency", "EUR");
        assertThat(updated.getDetails()).containsEntry("visaRequired", true);
        assertThat(updated.getDetails()).containsEntry("timezone", "GMT+2");
        verify(cacheInvalidationService).evictDestinationCaches(1L);
        verify(mongoEventLogger).onEvent(eq("DETAILS_UPDATED"), any());
    }

    @Test
    void updateDetails_destinationNotFound_throws404() {
        when(destinationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> destinationService.updateDetails(99L, Map.of("currency", "EUR")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
        verify(destinationRepository, never()).save(any());
    }

    @Test
    void updateDetails_nullIncomingDetails_throws400() {
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(newDestination(1L)));
        assertThatThrownBy(() -> destinationService.updateDetails(1L, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).save(any());
    }

    @Test
    void updateDetails_nullExistingDetails_setsIncoming() {
        Destination destination = newDestination(1L);
        destination.setDetails(null);
        Map<String, Object> incoming = new HashMap<>();
        incoming.put("climate", "tropical");
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));
        Destination updated = destinationService.updateDetails(1L, incoming);
        assertThat(updated.getDetails()).containsEntry("climate", "tropical");
    }

    // =========================================================================
    // S2-F4: Update Destination Status
    // M3: INACTIVE guard uses Feign active-count; publishes destination.status-changed event
    // =========================================================================

    @Test
    void updateStatus_blankStatus_throws400() {
        assertThatThrownBy(() -> destinationService.updateStatus(1L, "   "))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).findById(any());
        verify(itineraryServiceClient, never()).getDestinationActiveItineraryCount(anyLong());
    }

    @Test
    void updateStatus_invalidStatus_throws400() {
        assertThatThrownBy(() -> destinationService.updateStatus(1L, "RETIRED"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).findById(any());
    }

    @Test
    void updateStatus_destinationNotFound_throws404() {
        when(destinationRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> destinationService.updateStatus(9L, "ACTIVE"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void updateStatus_inactiveWithActiveItineraries_throws400() {
        // M3: Feign returns active-count > 0 → 400, nothing saved, no event published
        Destination dest = newDestination(1L);
        dest.setStatus(Destination.Status.ACTIVE);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getDestinationActiveItineraryCount(1L)).thenReturn(1);

        assertThatThrownBy(() -> destinationService.updateStatus(1L, "INACTIVE"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).save(any());
        verify(destinationEventPublisher, never()).publishStatusChanged(any());
    }

    @Test
    void updateStatus_inactiveWhenNoActiveItineraries_savesAndPublishesEvent() {
        // M3: Feign returns 0 → allowed; status persisted, event published
        Destination dest = newDestination(3L);
        dest.setStatus(Destination.Status.ACTIVE);
        when(destinationRepository.findById(3L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getDestinationActiveItineraryCount(3L)).thenReturn(0);
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));

        Destination updated = destinationService.updateStatus(3L, "INACTIVE");

        assertThat(updated.getStatus()).isEqualTo(Destination.Status.INACTIVE);
        verify(itineraryServiceClient).getDestinationActiveItineraryCount(3L);
        verify(cacheInvalidationService).evictDestinationCaches(3L);
        verify(mongoEventLogger).onEvent(eq("STATUS_CHANGED"), any());
        verify(destinationEventPublisher).publishStatusChanged(any());
    }

    @Test
    void updateStatus_toActive_noFeignCall_savesAndPublishesEvent() {
        // ACTIVE transition: Feign must NEVER be called — no guard needed per spec
        Destination dest = newDestination(2L);
        dest.setStatus(Destination.Status.INACTIVE);
        when(destinationRepository.findById(2L)).thenReturn(Optional.of(dest));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));

        Destination updated = destinationService.updateStatus(2L, "ACTIVE");

        assertThat(updated.getStatus()).isEqualTo(Destination.Status.ACTIVE);
        verify(itineraryServiceClient, never()).getDestinationActiveItineraryCount(anyLong());
        verify(cacheInvalidationService).evictDestinationCaches(2L);
        verify(mongoEventLogger).onEvent(eq("STATUS_CHANGED"), any());
        verify(destinationEventPublisher).publishStatusChanged(any());
    }

    @Test
    void updateStatus_toSeasonal_noFeignCall_savesAndPublishesEvent() {
        // SEASONAL transition: same as ACTIVE — Feign guard skipped
        Destination dest = newDestination(4L);
        when(destinationRepository.findById(4L)).thenReturn(Optional.of(dest));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));

        Destination updated = destinationService.updateStatus(4L, "SEASONAL");

        assertThat(updated.getStatus()).isEqualTo(Destination.Status.SEASONAL);
        verify(itineraryServiceClient, never()).getDestinationActiveItineraryCount(anyLong());
        verify(mongoEventLogger).onEvent(eq("STATUS_CHANGED"), any());
        verify(destinationEventPublisher).publishStatusChanged(any());
    }

    @Test
    void updateStatus_statusIsCaseInsensitive() {
        // "inactive" lowercase must be treated identically to "INACTIVE"
        Destination dest = newDestination(5L);
        when(destinationRepository.findById(5L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getDestinationActiveItineraryCount(5L)).thenReturn(0);
        when(destinationRepository.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));

        Destination updated = destinationService.updateStatus(5L, "inactive");

        assertThat(updated.getStatus()).isEqualTo(Destination.Status.INACTIVE);
    }

    @Test
    void updateStatus_publishedEvent_containsOldAndNewStatus() {
        Destination dest = newDestination(1L);
        dest.setStatus(Destination.Status.ACTIVE);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));

        destinationService.updateStatus(1L, "SEASONAL");

        ArgumentCaptor<StatusChangedEvent> captor = ArgumentCaptor.forClass(StatusChangedEvent.class);
        verify(destinationEventPublisher).publishStatusChanged(captor.capture());
        assertThat(captor.getValue().destinationId()).isEqualTo(1L);
        assertThat(captor.getValue().oldStatus()).isEqualTo("ACTIVE");
        assertThat(captor.getValue().newStatus()).isEqualTo("SEASONAL");
    }

    /**
     * S2-F4 full scenario (spec steps 2–6):
     * Step 2-3: PUT INACTIVE → Feign active-count=1 → 400, nothing saved.
     * Step 4-5: Cancel itinerary → Feign active-count=0 → 200, INACTIVE, event published.
     * Step 6:   PUT ACTIVE → no Feign call → 200, ACTIVE, event published.
     */
    @Test
    void updateStatus_s2f4_fullScenario() {
        Destination dest = newDestination(1L);
        dest.setStatus(Destination.Status.ACTIVE);

        // Step 2-3: INACTIVE blocked (active-count=1)
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getDestinationActiveItineraryCount(1L)).thenReturn(1);

        assertThatThrownBy(() -> destinationService.updateStatus(1L, "INACTIVE"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).save(any());
        verify(destinationEventPublisher, never()).publishStatusChanged(any());

        // Step 4-5: itinerary cancelled → active-count=0 → INACTIVE allowed
        when(itineraryServiceClient.getDestinationActiveItineraryCount(1L)).thenReturn(0);
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));

        Destination afterInactive = destinationService.updateStatus(1L, "INACTIVE");
        assertThat(afterInactive.getStatus()).isEqualTo(Destination.Status.INACTIVE);
        verify(destinationEventPublisher, times(1)).publishStatusChanged(any());

        // Step 6: PUT ACTIVE → no Feign call → 200, event published
        dest.setStatus(Destination.Status.INACTIVE);

        Destination afterActive = destinationService.updateStatus(1L, "ACTIVE");
        assertThat(afterActive.getStatus()).isEqualTo(Destination.Status.ACTIVE);
        // Feign called exactly twice: steps 2-3 and 4-5 (never for ACTIVE)
        verify(itineraryServiceClient, times(2)).getDestinationActiveItineraryCount(1L);
        verify(destinationEventPublisher, times(2)).publishStatusChanged(any());
    }

    // =========================================================================
    // S2-F5: Filter Destinations by Detail Attribute (JSONB search)
    // =========================================================================

    @Test
    void searchByDetails_blankKey_throws400() {
        assertThatThrownBy(() -> destinationService.searchByDetailsKeyValue(" ", "tropical", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).searchByDetailsKeyValue(any(), any(), any());
    }

    @Test
    void searchByDetails_blankValue_throws400() {
        assertThatThrownBy(() -> destinationService.searchByDetailsKeyValue("climate", " ", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).searchByDetailsKeyValue(any(), any(), any());
    }

    @Test
    void searchByDetails_invalidStatus_throws400() {
        assertThatThrownBy(() -> destinationService.searchByDetailsKeyValue("climate", "tropical", "BAD"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).searchByDetailsKeyValue(any(), any(), any());
    }

    @Test
    void searchByDetails_noStatus_passesNullStatusFilter() {
        when(destinationRepository.searchByDetailsKeyValue("climate", "tropical", null)).thenReturn(List.of());
        assertThat(destinationService.searchByDetailsKeyValue("climate", "tropical", null)).isEmpty();
        verify(destinationRepository).searchByDetailsKeyValue("climate", "tropical", null);
    }

    @Test
    void searchByDetails_withStatus_passesNormalizedUpperCaseStatus() {
        when(destinationRepository.searchByDetailsKeyValue("climate", "tropical", "ACTIVE")).thenReturn(List.of());
        destinationService.searchByDetailsKeyValue("climate", "tropical", "active");
        verify(destinationRepository).searchByDetailsKeyValue(eq("climate"), eq("tropical"), eq("ACTIVE"));
    }

    @Test
    void searchByDetails_returnsMatchingDestinations() {
        Destination d1 = newDestination(1L);
        Destination d2 = newDestination(2L);
        when(destinationRepository.searchByDetailsKeyValue("climate", "tropical", null))
                .thenReturn(List.of(d1, d2));
        List<Destination> results = destinationService.searchByDetailsKeyValue("climate", "tropical", null);
        assertThat(results).hasSize(2);
    }

    @Test
    void searchByDetails_noResults_returnsEmptyList() {
        when(destinationRepository.searchByDetailsKeyValue("climate", "arctic", null)).thenReturn(List.of());
        assertThat(destinationService.searchByDetailsKeyValue("climate", "arctic", null)).isEmpty();
    }

    // =========================================================================
    // S2-F6: Top Rated Destinations Report
    // M3: totalBookings proxied from Destination.totalRatings (no cross-service Feign)
    // =========================================================================

    @Test
    void topRatedReport_limitBelowOne_throws400() {
        assertThatThrownBy(() -> destinationService.getTopRatedDestinationsReport(0))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).findTopRatedDestinationsReport(anyInt());
    }

    @Test
    void topRatedReport_mapsRowsToDto() {
        Object[] row = new Object[]{10L, "Luxor", 4.9, 5L};
        when(destinationRepository.findTopRatedDestinationsReport(2)).thenReturn(List.<Object[]>of(row));
        List<TopDestinationDTO> list = destinationService.getTopRatedDestinationsReport(2);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getDestinationId()).isEqualTo(10L);
        assertThat(list.get(0).getName()).isEqualTo("Luxor");
        assertThat(list.get(0).getRating()).isEqualTo(4.9);
        assertThat(list.get(0).getTotalBookings()).isEqualTo(5L);
    }

    @Test
    void topRatedReport_nullRating_mapsToZero() {
        Object[] row = new Object[]{1L, "X", null, 0L};
        when(destinationRepository.findTopRatedDestinationsReport(10)).thenReturn(List.<Object[]>of(row));
        assertThat(destinationService.getTopRatedDestinationsReport(10).get(0).getRating()).isEqualTo(0.0);
    }

    @Test
    void topRatedReport_multipleRows_preservesOrder() {
        Object[] r1 = new Object[]{1L, "Luxor", 4.9, 10L};
        Object[] r2 = new Object[]{2L, "Dahab", 4.5, 5L};
        Object[] r3 = new Object[]{3L, "Cairo", 4.2, 3L};
        when(destinationRepository.findTopRatedDestinationsReport(3)).thenReturn(List.<Object[]>of(r1, r2, r3));
        List<TopDestinationDTO> list = destinationService.getTopRatedDestinationsReport(3);
        assertThat(list).hasSize(3);
        assertThat(list.get(0).getName()).isEqualTo("Luxor");
        assertThat(list.get(2).getName()).isEqualTo("Cairo");
    }

    @Test
    void topRatedReport_returnsAllWhenLimitExceedsCount() {
        Object[] r1 = new Object[]{1L, "A", 4.9, 1L};
        Object[] r2 = new Object[]{2L, "B", 4.5, 1L};
        when(destinationRepository.findTopRatedDestinationsReport(10)).thenReturn(List.<Object[]>of(r1, r2));
        assertThat(destinationService.getTopRatedDestinationsReport(10)).hasSize(2);
    }

    // =========================================================================
    // S2-F7: Rate a Destination After Visit (POST /api/destinations/{id}/rate)
    // M3: Feign call to itinerary-service; accepts COMPLETED or PAID status;
    //     publishes destination.rated event
    // =========================================================================

    @Test
    void rateAfterVisit_destinationNotFound_throws404() {
        when(destinationRepository.findById(99L)).thenReturn(Optional.empty());
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(1L);
        req.setRating(5);
        assertThatThrownBy(() -> destinationService.rateAfterVisit(99L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
        verify(itineraryServiceClient, never()).getItinerary(any());
    }

    @Test
    void rateAfterVisit_ratingOutOfRange_throws400() {
        Destination d = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(1L);
        req.setRating(6);
        assertThatThrownBy(() -> destinationService.rateAfterVisit(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void rateAfterVisit_ratingZero_throws400() {
        Destination d = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(1L);
        req.setRating(0);
        assertThatThrownBy(() -> destinationService.rateAfterVisit(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void rateAfterVisit_ratingOne_isValidBoundary() {
        Destination d = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        when(itineraryServiceClient.getItinerary(5L))
                .thenReturn(new ItineraryDTO(5L, 1L, 1L, "PAID"));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(5L);
        req.setRating(1);
        Destination updated = destinationService.rateAfterVisit(1L, req);
        assertThat(updated.getRating()).isEqualTo(1.0);
    }

    @Test
    void rateAfterVisit_itineraryNotFound_throws404() {
        Destination d = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        when(itineraryServiceClient.getItinerary(10L)).thenThrow(feignNotFound());
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(10L);
        req.setRating(5);
        assertThatThrownBy(() -> destinationService.rateAfterVisit(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void rateAfterVisit_wrongDestination_throws400() {
        Destination d = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        // Itinerary references destinationId=2, but caller is rating destinationId=1
        when(itineraryServiceClient.getItinerary(10L))
                .thenReturn(new ItineraryDTO(10L, 2L, 1L, "COMPLETED"));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(10L);
        req.setRating(5);
        assertThatThrownBy(() -> destinationService.rateAfterVisit(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void rateAfterVisit_notCompleted_throws400() {
        Destination d = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        when(itineraryServiceClient.getItinerary(10L))
                .thenReturn(new ItineraryDTO(10L, 1L, 1L, "PLANNED"));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(10L);
        req.setRating(5);
        assertThatThrownBy(() -> destinationService.rateAfterVisit(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    /**
     * COMPLETING status must be rejected — spec says only COMPLETED and PAID are accepted in M3.
     */
    @Test
    void rateAfterVisit_completingStatus_throws400() {
        Destination d = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        when(itineraryServiceClient.getItinerary(10L))
                .thenReturn(new ItineraryDTO(10L, 1L, 1L, "COMPLETING"));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(10L);
        req.setRating(4);
        assertThatThrownBy(() -> destinationService.rateAfterVisit(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    /**
     * PAYMENT_PENDING status must be rejected — spec says only COMPLETED and PAID are accepted in M3.
     */
    @Test
    void rateAfterVisit_paymentPendingStatus_throws400() {
        Destination d = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        when(itineraryServiceClient.getItinerary(10L))
                .thenReturn(new ItineraryDTO(10L, 1L, 1L, "PAYMENT_PENDING"));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(10L);
        req.setRating(4);
        assertThatThrownBy(() -> destinationService.rateAfterVisit(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    /**
     * M3: PAID status is accepted as equivalent to COMPLETED (backward-compatible with pre-M3 data).
     */
    @Test
    void rateAfterVisit_paidStatus_isAccepted() {
        Destination d = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        when(itineraryServiceClient.getItinerary(7L))
                .thenReturn(new ItineraryDTO(7L, 1L, 42L, "PAID"));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(7L);
        req.setRating(4);
        Destination updated = destinationService.rateAfterVisit(1L, req);
        assertThat(updated.getRating()).isEqualTo(4.0);
        assertThat(updated.getTotalRatings()).isEqualTo(1);
        verify(destinationEventPublisher).publishRated(any());
    }

    @Test
    void rateAfterVisit_firstRating_setsAverageAndCount() {
        Destination d = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        when(itineraryServiceClient.getItinerary(10L))
                .thenReturn(new ItineraryDTO(10L, 1L, 99L, "COMPLETED"));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(10L);
        req.setRating(5);
        Destination updated = destinationService.rateAfterVisit(1L, req);
        assertThat(updated.getRating()).isEqualTo(5.0);
        assertThat(updated.getTotalRatings()).isEqualTo(1);
        verify(cacheInvalidationService).evictDestinationCaches(1L);
        verify(mongoEventLogger).onEvent(eq("RATING_ADDED"), any());
        verify(destinationEventPublisher).publishRated(any());
    }

    @Test
    void rateAfterVisit_secondRating_recalculatesRunningAverage() {
        Destination d = newDestination(1L);
        d.setRating(5.0);
        d.setTotalRatings(1);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        when(itineraryServiceClient.getItinerary(11L))
                .thenReturn(new ItineraryDTO(11L, 1L, 1L, "COMPLETED"));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(11L);
        req.setRating(3);
        Destination updated = destinationService.rateAfterVisit(1L, req);
        // (5.0 * 1 + 3) / 2 = 4.0
        assertThat(updated.getRating()).isEqualTo(4.0);
        assertThat(updated.getTotalRatings()).isEqualTo(2);
        verify(destinationEventPublisher).publishRated(any());
    }

    /**
     * S2-F7 full scenario (spec steps 2-8):
     * Step 2-3: rate=5, COMPLETED → rating=5.0, totalRatings=1, event published
     * Step 4:   rate=3, COMPLETED, different itinerary → rating=4.0, totalRatings=2
     * Step 5:   itinerary=99 not found → 404
     * Step 6:   itinerary=10 now PLANNED → 400
     * Step 7:   rating=6 → 400 (out of range)
     * Step 8:   itinerary references destinationId=2, caller rates dest=1 → 400
     */
    @Test
    void rateAfterVisit_s2f7_fullScenario() {
        // Step 2-3
        Destination dest = newDestination(1L);
        dest.setRating(0.0);
        dest.setTotalRatings(0);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getItinerary(10L))
                .thenReturn(new ItineraryDTO(10L, 1L, 99L, "COMPLETED"));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));

        DestinationRateRequest req1 = new DestinationRateRequest();
        req1.setItineraryId(10L);
        req1.setRating(5);
        Destination after1 = destinationService.rateAfterVisit(1L, req1);
        assertThat(after1.getRating()).isEqualTo(5.0);
        assertThat(after1.getTotalRatings()).isEqualTo(1);
        verify(destinationEventPublisher, times(1)).publishRated(any());

        // Step 4
        dest.setRating(5.0);
        dest.setTotalRatings(1);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getItinerary(11L))
                .thenReturn(new ItineraryDTO(11L, 1L, 100L, "COMPLETED"));

        DestinationRateRequest req2 = new DestinationRateRequest();
        req2.setItineraryId(11L);
        req2.setRating(3);
        Destination after2 = destinationService.rateAfterVisit(1L, req2);
        assertThat(after2.getRating()).isEqualTo(4.0);
        assertThat(after2.getTotalRatings()).isEqualTo(2);
        verify(destinationEventPublisher, times(2)).publishRated(any());

        // Step 5: itinerary=99 not found → 404
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getItinerary(99L)).thenThrow(feignNotFound());
        DestinationRateRequest req3 = new DestinationRateRequest();
        req3.setItineraryId(99L);
        req3.setRating(4);
        assertThatThrownBy(() -> destinationService.rateAfterVisit(1L, req3))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));

        // Step 6: itinerary=10 now PLANNED → 400
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getItinerary(10L))
                .thenReturn(new ItineraryDTO(10L, 1L, 99L, "PLANNED"));
        DestinationRateRequest req4 = new DestinationRateRequest();
        req4.setItineraryId(10L);
        req4.setRating(4);
        assertThatThrownBy(() -> destinationService.rateAfterVisit(1L, req4))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));

        // Step 7: rating=6 → 400
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        DestinationRateRequest req5 = new DestinationRateRequest();
        req5.setItineraryId(10L);
        req5.setRating(6);
        assertThatThrownBy(() -> destinationService.rateAfterVisit(1L, req5))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));

        // Step 8: itinerary references destinationId=2, caller is rating dest=1 → 400
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getItinerary(10L))
                .thenReturn(new ItineraryDTO(10L, 2L, 99L, "COMPLETED"));
        DestinationRateRequest req6 = new DestinationRateRequest();
        req6.setItineraryId(10L);
        req6.setRating(4);
        assertThatThrownBy(() -> destinationService.rateAfterVisit(1L, req6))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));

        // publishRated must still be exactly 2 (steps 2-3 and 4; rejected steps add none)
        verify(destinationEventPublisher, times(2)).publishRated(any());
    }

    // =========================================================================
    // S2-F8: Verify Destination Review (PUT /api/destinations/{id}/reviews/{id}/verify)
    // M3: ADMIN check via Feign call to user-service (replaces direct SQL on users)
    // =========================================================================

    @Test
    void verifyReview_destinationNotFound_throws404() {
        when(destinationRepository.findById(99L)).thenReturn(Optional.empty());
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        req.setVerifiedBy(3L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(99L, 10L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
        verify(userServiceClient, never()).getUser(any());
    }

    @Test
    void verifyReview_missingVerifiedBy_throws400() {
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(newDestination(1L)));
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        // verifiedBy not set — null
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 10L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void verifyReview_reviewNotFound_throws404() {
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(newDestination(1L)));
        when(destinationReviewRepository.findById(99L)).thenReturn(Optional.empty());
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        req.setVerifiedBy(3L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 99L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void verifyReview_wrongDestination_throws400() {
        Destination d1 = newDestination(1L);
        Destination d2 = newDestination(2L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d1));
        DestinationReview review = new DestinationReview();
        review.setId(10L);
        review.setDestination(d2);  // belongs to d2, not d1
        review.setVisitDate(LocalDate.now().minusDays(1));
        when(destinationReviewRepository.findById(10L)).thenReturn(Optional.of(review));
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        req.setVerifiedBy(3L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 10L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        // wrong-destination guard fires before Feign call
        verify(userServiceClient, never()).getUser(any());
    }

    /**
     * visitDate in future must throw 400 — and Feign must NOT be called (spec: "avoid wasting a downstream hop").
     */
    @Test
    void verifyReview_futureVisit_throws400_feignNeverCalled() {
        Destination d1 = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d1));
        DestinationReview review = new DestinationReview();
        review.setId(10L);
        review.setDestination(d1);
        review.setVisitDate(LocalDate.now().plusDays(1));  // future
        when(destinationReviewRepository.findById(10L)).thenReturn(Optional.of(review));
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        req.setVerifiedBy(3L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 10L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        // Feign must never be called — visitDate guard fires first
        verify(userServiceClient, never()).getUser(any());
    }

    /**
     * Today's visitDate is NOT future — should proceed to Feign (and succeed if ADMIN).
     */
    @Test
    void verifyReview_todayVisitDate_isNotFuture_succeeds() {
        Destination d1 = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d1));
        when(userServiceClient.getUser(3L)).thenReturn(new UserDTO(3L, "ADMIN"));
        DestinationReview review = new DestinationReview();
        review.setId(20L);
        review.setType(ReviewType.VISITOR);
        review.setContent("Good");
        review.setRating(4);
        review.setDestination(d1);
        review.setVisitDate(LocalDate.now());  // today is valid
        review.setVerified(false);
        when(destinationReviewRepository.findById(20L)).thenReturn(Optional.of(review));
        when(destinationReviewRepository.save(any(DestinationReview.class))).thenAnswer(i -> i.getArgument(0));
        Destination withReviews = newDestination(1L);
        withReviews.setDestinationReviews(List.of(review));
        when(destinationRepository.findByIdWithDestinationReviews(1L)).thenReturn(Optional.of(withReviews));
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        req.setVerifiedBy(3L);
        assertThat(destinationService.verifyDestinationReview(1L, 20L, req)).isNotNull();
    }

    /**
     * Feign returns 404 for userId → must surface as 403 (do not leak user existence).
     */
    @Test
    void verifyReview_userNotFound_throws403() {
        Destination d1 = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d1));
        DestinationReview review = new DestinationReview();
        review.setId(10L);
        review.setDestination(d1);
        review.setVisitDate(LocalDate.now().minusDays(1));
        when(destinationReviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(userServiceClient.getUser(999L)).thenThrow(feignNotFound());
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        req.setVerifiedBy(999L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 10L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }

    /**
     * TRAVELER role must be rejected with 403 (spec step 4).
     */
    @Test
    void verifyReview_travelerRole_throws403() {
        Destination d1 = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d1));
        DestinationReview review = new DestinationReview();
        review.setId(10L);
        review.setDestination(d1);
        review.setVisitDate(LocalDate.now().minusDays(1));
        when(destinationReviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(userServiceClient.getUser(1L)).thenReturn(new UserDTO(1L, "TRAVELER"));
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        req.setVerifiedBy(1L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 10L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void verifyReview_success_setsVerifiedAndMetadata() {
        Destination d1 = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d1));
        when(userServiceClient.getUser(3L)).thenReturn(new UserDTO(3L, "ADMIN"));
        DestinationReview review = new DestinationReview();
        review.setId(10L);
        review.setType(ReviewType.VISITOR);
        review.setContent("Great");
        review.setRating(5);
        review.setDestination(d1);
        review.setVisitDate(LocalDate.now().minusDays(2));
        review.setVerified(false);
        when(destinationReviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(destinationReviewRepository.save(any(DestinationReview.class))).thenAnswer(i -> i.getArgument(0));
        Destination withReviews = newDestination(1L);
        withReviews.setDestinationReviews(List.of(review));
        when(destinationRepository.findByIdWithDestinationReviews(1L)).thenReturn(Optional.of(withReviews));
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        req.setVerifiedBy(3L);

        destinationService.verifyDestinationReview(1L, 10L, req);

        ArgumentCaptor<DestinationReview> captor = ArgumentCaptor.forClass(DestinationReview.class);
        verify(destinationReviewRepository).save(captor.capture());
        assertThat(captor.getValue().getVerified()).isTrue();
        assertThat(captor.getValue().getMetadata()).containsKeys("verifiedAt", "verifiedBy");
        assertThat(captor.getValue().getMetadata().get("verifiedBy")).isEqualTo(3L);
        verify(cacheInvalidationService).evictDestinationReviewCaches(1L, 10L);
        verify(mongoEventLogger).onEvent(eq("REVIEW_VERIFIED"), any());
    }

    /**
     * S2-F8 full scenario (spec steps 2-7):
     * Step 2-3: ADMIN user verifies review → 200, verified=true, metadata: verifiedAt + verifiedBy=3
     * Step 4:   TRAVELER role → 403
     * Step 5:   Non-existent user (Feign 404) → 403
     * Step 6:   visitDate in future → 400, Feign never called
     * Step 7:   Review belongs to different destination → 400
     */
    @Test
    void verifyReview_s2f8_fullScenario() {
        Destination dest1 = newDestination(1L);
        Destination dest2 = newDestination(2L);

        DestinationReview review5 = new DestinationReview();
        review5.setId(5L);
        review5.setType(ReviewType.VISITOR);
        review5.setContent("Wonderful place");
        review5.setRating(5);
        review5.setDestination(dest1);
        review5.setVisitDate(LocalDate.of(2026, 1, 15));
        review5.setVerified(false);

        // Step 2-3: ADMIN → 200, verified=true, metadata populated
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest1));
        when(destinationReviewRepository.findById(5L)).thenReturn(Optional.of(review5));
        when(userServiceClient.getUser(3L)).thenReturn(new UserDTO(3L, "ADMIN"));
        when(destinationReviewRepository.save(any(DestinationReview.class))).thenAnswer(i -> i.getArgument(0));
        Destination withReviews = newDestination(1L);
        withReviews.setDestinationReviews(List.of(review5));
        when(destinationRepository.findByIdWithDestinationReviews(1L)).thenReturn(Optional.of(withReviews));

        VerifyDestinationReviewRequest req1 = new VerifyDestinationReviewRequest();
        req1.setVerifiedBy(3L);
        destinationService.verifyDestinationReview(1L, 5L, req1);

        ArgumentCaptor<DestinationReview> captor = ArgumentCaptor.forClass(DestinationReview.class);
        verify(destinationReviewRepository).save(captor.capture());
        assertThat(captor.getValue().getVerified()).isTrue();
        assertThat(captor.getValue().getMetadata()).containsKey("verifiedAt");
        assertThat(captor.getValue().getMetadata().get("verifiedBy")).isEqualTo(3L);

        // Step 4: TRAVELER role → 403
        review5.setVerified(false);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest1));
        when(destinationReviewRepository.findById(5L)).thenReturn(Optional.of(review5));
        when(userServiceClient.getUser(1L)).thenReturn(new UserDTO(1L, "TRAVELER"));

        VerifyDestinationReviewRequest req2 = new VerifyDestinationReviewRequest();
        req2.setVerifiedBy(1L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 5L, req2))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));

        // Step 5: non-existent user (Feign 404) → 403
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest1));
        when(destinationReviewRepository.findById(5L)).thenReturn(Optional.of(review5));
        when(userServiceClient.getUser(999L)).thenThrow(feignNotFound());

        VerifyDestinationReviewRequest req3 = new VerifyDestinationReviewRequest();
        req3.setVerifiedBy(999L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 5L, req3))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));

        // Step 6: visitDate in future → 400, Feign must NOT be called with user=3 again
        DestinationReview futureReview = new DestinationReview();
        futureReview.setId(5L);
        futureReview.setDestination(dest1);
        futureReview.setVisitDate(LocalDate.now().plusDays(10));
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest1));
        when(destinationReviewRepository.findById(5L)).thenReturn(Optional.of(futureReview));

        VerifyDestinationReviewRequest req4 = new VerifyDestinationReviewRequest();
        req4.setVerifiedBy(3L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 5L, req4))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        // visitDate guard fires before Feign — user 3 must have been fetched only once (step 2-3)
        verify(userServiceClient, times(1)).getUser(3L);

        // Step 7: review belongs to dest2 but caller is verifying dest1 → 400
        DestinationReview wrongDestReview = new DestinationReview();
        wrongDestReview.setId(5L);
        wrongDestReview.setDestination(dest2);
        wrongDestReview.setVisitDate(LocalDate.now().minusDays(1));
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest1));
        when(destinationReviewRepository.findById(5L)).thenReturn(Optional.of(wrongDestReview));

        VerifyDestinationReviewRequest req5 = new VerifyDestinationReviewRequest();
        req5.setVerifiedBy(3L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 5L, req5))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        // wrong-destination check also fires before Feign — user 3 still fetched only once total
        verify(userServiceClient, times(1)).getUser(3L);
    }

    // =========================================================================
    // S2-F9: Get Destinations With Low-Rated Reviews
    // =========================================================================

    @Test
    void lowRatedReviews_negativeMax_throws400() {
        assertThatThrownBy(() -> destinationService.getDestinationsWithLowRatedReviews(-1))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationReviewRepository, never()).findLowRatedReviewsWithDestination(anyInt());
    }

    @Test
    void lowRatedReviews_noMatches_returnsEmpty() {
        when(destinationReviewRepository.findLowRatedReviewsWithDestination(0)).thenReturn(List.of());
        assertThat(destinationService.getDestinationsWithLowRatedReviews(0)).isEmpty();
    }

    @Test
    void lowRatedReviews_groupsByDestinationWithCounts() {
        Destination d1 = newDestination(1L);
        Destination d3 = newDestination(3L);
        DestinationReview r1 = lowRatedReview(1L, d1, 2);
        DestinationReview r2 = lowRatedReview(2L, d1, 1);
        DestinationReview r3 = lowRatedReview(3L, d3, 2);
        when(destinationReviewRepository.findLowRatedReviewsWithDestination(2))
                .thenReturn(List.of(r1, r2, r3));
        List<DestinationReviewAlertDTO> list = destinationService.getDestinationsWithLowRatedReviews(2);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getDestinationId()).isEqualTo(1L);
        assertThat(list.get(0).getLowRatedCount()).isEqualTo(2);
        assertThat(list.get(0).getLowRatedReviews()).hasSize(2);
        assertThat(list.get(1).getDestinationId()).isEqualTo(3L);
        assertThat(list.get(1).getLowRatedCount()).isEqualTo(1);
    }

    @Test
    void lowRatedReviews_dtoContainsDestinationNameAndStatus() {
        Destination d = newDestination(10L);
        d.setName("Karnak");
        DestinationReview r = lowRatedReview(1L, d, 1);
        when(destinationReviewRepository.findLowRatedReviewsWithDestination(2)).thenReturn(List.of(r));
        List<DestinationReviewAlertDTO> list = destinationService.getDestinationsWithLowRatedReviews(2);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getDestinationName()).isEqualTo("Karnak");
        assertThat(list.get(0).getDestinationStatus()).isEqualTo("ACTIVE");
    }

    // =========================================================================
    // S2-F1: Search by Category and Rating Range
    // =========================================================================

    @Test
    void searchByCategoryAndRatingRange_withCategory_returnsFilteredAndOrderedDestinations() {
        Destination d1 = newDestination(1L);
        d1.setRating(4.5);
        Destination d3 = newDestination(3L);
        d3.setRating(4.9);
        when(destinationRepository.searchByCategoryAndRatingRange("BEACH", 4.0, 5.0))
                .thenReturn(List.of(d3, d1));
        List<Destination> result = destinationService.searchByCategoryAndRatingRange("BEACH", 4.0, 5.0);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRating()).isEqualTo(4.9);
        assertThat(result.get(1).getRating()).isEqualTo(4.5);
    }

    @Test
    void searchByCategoryAndRatingRange_withoutCategory_returnsFilteredDestinations() {
        Destination d2 = newDestination(2L);
        d2.setRating(3.8);
        when(destinationRepository.searchByCategoryAndRatingRange(null, 3.0, 4.0)).thenReturn(List.of(d2));
        List<Destination> result = destinationService.searchByCategoryAndRatingRange(null, 3.0, 4.0);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRating()).isEqualTo(3.8);
    }

    @Test
    void searchByCategoryAndRatingRange_invalidRange_throwsBadRequest() {
        assertThatThrownBy(() -> destinationService.searchByCategoryAndRatingRange(null, 5.0, 3.0))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("minRating cannot be greater than maxRating");
        verify(destinationRepository, never()).searchByCategoryAndRatingRange(any(), any(), any());
    }

    @Test
    void searchByCategoryAndRatingRange_missingMinRating_throwsBadRequest() {
        assertThatThrownBy(() -> destinationService.searchByCategoryAndRatingRange(null, null, 5.0))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("minRating and maxRating are required");
    }

    @Test
    void searchByCategoryAndRatingRange_missingMaxRating_throwsBadRequest() {
        assertThatThrownBy(() -> destinationService.searchByCategoryAndRatingRange(null, 3.0, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("minRating and maxRating are required");
    }

    // =========================================================================
    // Observer Pattern Tests
    // =========================================================================

    @Test
    void register_addsObserver_andNotifies() {
        destinationService.register(mockObserver);
        Destination destination = newDestination(1L);
        destination.setDetails(new HashMap<>(Map.of("climate", "tropical")));
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));
        destinationService.updateDetails(1L, Map.of("currency", "EUR"));
        verify(mockObserver).onEvent(eq("DETAILS_UPDATED"), any());
        verify(mongoEventLogger).onEvent(eq("DETAILS_UPDATED"), any());
    }

    @Test
    void register_nullObserver_ignored() {
        destinationService.register(null);
        Destination destination = newDestination(1L);
        destination.setDetails(new HashMap<>(Map.of("climate", "tropical")));
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));
        destinationService.updateDetails(1L, Map.of("currency", "EUR"));
        verify(mockObserver, never()).onEvent(any(), any());
    }

    @Test
    void register_duplicateObserver_notAddedTwice() {
        destinationService.register(mockObserver);
        destinationService.register(mockObserver);
        Destination destination = newDestination(1L);
        destination.setDetails(new HashMap<>(Map.of("climate", "tropical")));
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));
        destinationService.updateDetails(1L, Map.of("currency", "EUR"));
        // Must notify exactly once despite double registration
        verify(mockObserver, times(1)).onEvent(eq("DETAILS_UPDATED"), any());
    }

    @Test
    void unregister_removesObserver_noLongerNotified() {
        destinationService.register(mockObserver);
        destinationService.unregister(mockObserver);
        Destination destination = newDestination(1L);
        destination.setDetails(new HashMap<>(Map.of("climate", "tropical")));
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));
        destinationService.updateDetails(1L, Map.of("currency", "EUR"));
        verify(mockObserver, never()).onEvent(any(), any());
    }

    @Test
    void unregister_notRegistered_doesNothing() {
        destinationService.unregister(mockObserver);
        Destination destination = newDestination(1L);
        destination.setDetails(new HashMap<>(Map.of("climate", "tropical")));
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));
        destinationService.updateDetails(1L, Map.of("currency", "EUR"));
        verify(mockObserver, never()).onEvent(any(), any());
    }

    // =========================================================================
    // S2-F11: Index Destination
    // =========================================================================

    @Test
    void indexDestination_firesIndexedEvent() {
        Destination dest = newDestination(7L);
        dest.setName("Marsa Alam");
        when(destinationRepository.findById(7L)).thenReturn(Optional.of(dest));
        destinationService.indexDestination(7L);
        verify(mongoEventLogger).onEvent(eq("INDEXED"), any());
    }

    @Test
    void indexDestination_destinationNotFound_throws404() {
        when(destinationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> destinationService.indexDestination(999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    // =========================================================================
    // CRUD write events via Observer
    // =========================================================================

    @Test
    void createDestination_firesDestinationCreatedEvent() {
        Destination dest = newDestination(null);
        when(destinationRepository.save(any(Destination.class))).thenReturn(dest);
        destinationService.createDestination(dest);
        verify(mongoEventLogger).onEvent(eq("DESTINATION_CREATED"), any());
        verify(cacheInvalidationService).evictDestinationCaches(any());
    }

    @Test
    void updateDestination_firesDestinationUpdatedEvent() {
        Destination existing = newDestination(1L);
        Destination patch = new Destination();
        patch.setName("New Name");
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));
        destinationService.updateDestination(1L, patch);
        verify(mongoEventLogger).onEvent(eq("DESTINATION_UPDATED"), any());
        verify(cacheInvalidationService).evictDestinationCaches(1L);
    }

    @Test
    void deleteDestination_firesDestinationDeletedEvent() {
        Destination existing = newDestination(5L);
        when(destinationRepository.findById(5L)).thenReturn(Optional.of(existing));
        destinationService.deleteDestination(5L);
        verify(mongoEventLogger).onEvent(eq("DESTINATION_DELETED"), any());
        verify(cacheInvalidationService).evictDestinationCaches(5L);
    }

    // =========================================================================
    // S2-F12: Get Destination Analytics Dashboard
    // M3: itinerary aggregation via Feign; DASHBOARD_VIEWED always logged; 10-min cache
    // =========================================================================

    @Test
    void dashboard_destinationNotFound_throws404() {
        when(destinationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> destinationService.getDestinationDashboard(999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
        // 404 path must not reach Feign
        verify(itineraryServiceClient, never()).getDestinationDashboardAggregate(anyLong());
    }

    @Test
    void dashboard_returnsCorrectAggregation() {
        Destination dest = newDestination(1L);
        dest.setName("Luxor");
        dest.setRating(4.5);
        dest.setTotalRatings(2);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getDestinationDashboardAggregate(1L))
                .thenReturn(new DestinationDashboardAggregateDTO(5L, 3L, 3L));
        DestinationDashboardDTO dto = destinationService.getDestinationDashboard(1L);
        assertThat(dto.getDestinationId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Luxor");
        assertThat(dto.getTotalItineraries()).isEqualTo(5L);
        assertThat(dto.getCompletedItineraries()).isEqualTo(3L);
        assertThat(dto.getTotalVisitors()).isEqualTo(3L);
        assertThat(dto.getTotalRatings()).isEqualTo(2);
        assertThat(dto.getAverageRating()).isEqualTo(4.5);
    }

    @Test
    void dashboard_noItineraries_returnsZeroes() {
        Destination dest = newDestination(5L);
        dest.setName("Empty");
        when(destinationRepository.findById(5L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getDestinationDashboardAggregate(5L))
                .thenReturn(new DestinationDashboardAggregateDTO(0L, 0L, 0L));
        DestinationDashboardDTO dto = destinationService.getDestinationDashboard(5L);
        assertThat(dto.getTotalItineraries()).isEqualTo(0L);
        assertThat(dto.getCompletedItineraries()).isEqualTo(0L);
        assertThat(dto.getTotalVisitors()).isEqualTo(0L);
    }

    @Test
    void dashboard_nullAggregateFromFeign_returnsZeroes() {
        Destination dest = newDestination(3L);
        when(destinationRepository.findById(3L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getDestinationDashboardAggregate(3L)).thenReturn(null);
        DestinationDashboardDTO dto = destinationService.getDestinationDashboard(3L);
        assertThat(dto.getTotalItineraries()).isEqualTo(0L);
        assertThat(dto.getCompletedItineraries()).isEqualTo(0L);
        assertThat(dto.getTotalVisitors()).isEqualTo(0L);
    }

    /**
     * DASHBOARD_VIEWED must be logged on every invocation — including cache hits.
     * The observer notify is called before the cache check so it can never be skipped.
     */
    @Test
    void dashboard_logsDashboardViewedOnEveryCall_includingCacheHits() {
        Destination dest = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getDestinationDashboardAggregate(1L))
                .thenReturn(new DestinationDashboardAggregateDTO(0L, 0L, 0L));
        destinationService.getDestinationDashboard(1L);
        destinationService.getDestinationDashboard(1L);
        // DASHBOARD_VIEWED must fire on every call, regardless of cache state
        verify(mongoEventLogger, times(2)).onEvent(eq("DASHBOARD_VIEWED"), any());
    }

    @Test
    void dashboard_delegatesToFeignForAggregation_notLocalDB() {
        Destination dest = newDestination(5L);
        when(destinationRepository.findById(5L)).thenReturn(Optional.of(dest));
        when(itineraryServiceClient.getDestinationDashboardAggregate(5L))
                .thenReturn(new DestinationDashboardAggregateDTO(5L, 3L, 3L));

        destinationService.getDestinationDashboard(5L);

        // Aggregation must come from Feign (itinerary-service), not a local DB query
        verify(itineraryServiceClient).getDestinationDashboardAggregate(5L);
    }

    /**
     * S2-F12 full scenario (spec steps 2-5):
     * Destination "Luxor" ID=5, rating=4.9, totalRatings=2.
     * itinerary-postgres: 5 itineraries (3 PAID by 3 distinct users → completedItineraries=3, totalVisitors=3).
     * Step 2-3: GET /api/destinations/5/dashboard → totalItineraries=5, completedItineraries=3, totalVisitors=3.
     * Step 4:   DASHBOARD_VIEWED logged on every call including cache hits.
     * Step 5:   GET /api/destinations/999/dashboard → 404.
     */
    @Test
    void dashboard_s2f12_fullScenario() {
        Destination luxor = newDestination(5L);
        luxor.setName("Luxor");
        luxor.setRating(4.9);
        luxor.setTotalRatings(2);
        when(destinationRepository.findById(5L)).thenReturn(Optional.of(luxor));
        when(itineraryServiceClient.getDestinationDashboardAggregate(5L))
                .thenReturn(new DestinationDashboardAggregateDTO(5L, 3L, 3L));

        DestinationDashboardDTO dto = destinationService.getDestinationDashboard(5L);

        assertThat(dto.getDestinationId()).isEqualTo(5L);
        assertThat(dto.getName()).isEqualTo("Luxor");
        assertThat(dto.getTotalItineraries()).isEqualTo(5L);
        assertThat(dto.getCompletedItineraries()).isEqualTo(3L);
        assertThat(dto.getTotalVisitors()).isEqualTo(3L);
        assertThat(dto.getTotalRatings()).isEqualTo(2);
        assertThat(dto.getAverageRating()).isEqualTo(4.9);
        verify(itineraryServiceClient).getDestinationDashboardAggregate(5L);
        // Step 4: DASHBOARD_VIEWED logged (first call)
        verify(mongoEventLogger, times(1)).onEvent(eq("DASHBOARD_VIEWED"), any());

        // Step 4: second call still logs DASHBOARD_VIEWED
        destinationService.getDestinationDashboard(5L);
        verify(mongoEventLogger, times(2)).onEvent(eq("DASHBOARD_VIEWED"), any());

        // Step 5: dest=999 → 404
        when(destinationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> destinationService.getDestinationDashboard(999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
        verify(itineraryServiceClient, never()).getDestinationDashboardAggregate(999L);
    }

    // =========================================================================
    // DP-2 Observer Pattern — Reflection
    // =========================================================================

    @Test
    void dp2_entityObserver_isInterface() {
        assertThat(EntityObserver.class.isInterface()).isTrue();
    }

    @Test
    void dp2_entityObserver_hasOnEventMethod() throws Exception {
        Method method = EntityObserver.class.getMethod("onEvent", String.class, Object.class);
        assertThat(Modifier.isAbstract(method.getModifiers())).isTrue();
    }

    @Test
    void dp2_mongoEventLogger_implementsEntityObserver() {
        assertThat(EntityObserver.class.isAssignableFrom(MongoEventLogger.class)).isTrue();
    }

    // =========================================================================
    // DP-3 Builder Pattern — Reflection
    // =========================================================================

    @Test
    void dp3_destinationDashboardDTO_hasStaticBuilderMethod() throws Exception {
        Method m = DestinationDashboardDTO.class.getMethod("builder");
        assertThat(Modifier.isStatic(m.getModifiers())).isTrue();
    }

    @Test
    void dp3_destinationRevenueDTO_hasStaticBuilderMethod() throws Exception {
        Method m = DestinationRevenueDTO.class.getMethod("builder");
        assertThat(Modifier.isStatic(m.getModifiers())).isTrue();
    }

    @Test
    void dp3_topDestinationDTO_hasStaticBuilderMethod() throws Exception {
        Method m = TopDestinationDTO.class.getMethod("builder");
        assertThat(Modifier.isStatic(m.getModifiers())).isTrue();
    }

    @Test
    void dp3_destinationReviewAlertDTO_hasStaticBuilderMethod() throws Exception {
        Method m = DestinationReviewAlertDTO.class.getMethod("builder");
        assertThat(Modifier.isStatic(m.getModifiers())).isTrue();
    }

    @Test
    void dp3_destinationDashboardDTO_builderFluentAndBuildReturnsDTO() throws Exception {
        Object builder = DestinationDashboardDTO.builder();
        Method nameSetter = builder.getClass().getMethod("name", String.class);
        Object ret = nameSetter.invoke(builder, "Test");
        assertThat(ret).isSameAs(builder);
        Object dto = builder.getClass().getMethod("build").invoke(builder);
        assertThat(dto).isInstanceOf(DestinationDashboardDTO.class);
    }

    @Test
    void dp3_destinationRevenueDTO_builderBuildReturnsCorrectType() throws Exception {
        Object builder = DestinationRevenueDTO.builder();
        Object dto = builder.getClass().getMethod("build").invoke(builder);
        assertThat(dto).isInstanceOf(DestinationRevenueDTO.class);
    }

    @Test
    void dp3_topDestinationDTO_builderBuildReturnsCorrectType() throws Exception {
        Object builder = TopDestinationDTO.builder();
        Object dto = builder.getClass().getMethod("build").invoke(builder);
        assertThat(dto).isInstanceOf(TopDestinationDTO.class);
    }

    @Test
    void dp3_dashboardDTO_allFieldsSetViaBuilder() {
        DestinationDashboardDTO dto = DestinationDashboardDTO.builder()
                .destinationId(42L).name("Dahab")
                .totalItineraries(5L).completedItineraries(3L)
                .totalVisitors(3L).totalRatings(2).averageRating(4.5)
                .build();
        assertThat(dto.getDestinationId()).isEqualTo(42L);
        assertThat(dto.getName()).isEqualTo("Dahab");
        assertThat(dto.getTotalItineraries()).isEqualTo(5L);
        assertThat(dto.getCompletedItineraries()).isEqualTo(3L);
        assertThat(dto.getTotalVisitors()).isEqualTo(3L);
        assertThat(dto.getTotalRatings()).isEqualTo(2);
        assertThat(dto.getAverageRating()).isEqualTo(4.5);
    }

    // =========================================================================
    // DP-3 Chain of Responsibility — Reflection
    // =========================================================================

    @Test
    void dp3_authHandler_isAbstract() {
        assertThat(Modifier.isAbstract(AuthHandler.class.getModifiers())).isTrue();
    }

    @Test
    void dp3_authHandler_hasSetNextMethod() throws Exception {
        Method setNext = AuthHandler.class.getMethod("setNext", AuthHandler.class);
        assertThat(setNext).isNotNull();
    }

    @Test
    void dp3_authHandler_hasHandleMethod() throws Exception {
        Method handle = AuthHandler.class.getMethod("handle", AuthContext.class);
        assertThat(handle).isNotNull();
    }

    @Test
    void dp3_tokenExtractionHandler_extendsAuthHandler() {
        assertThat(AuthHandler.class.isAssignableFrom(TokenExtractionHandler.class)).isTrue();
    }

    @Test
    void dp3_signatureValidationHandler_extendsAuthHandler() {
        assertThat(AuthHandler.class.isAssignableFrom(SignatureValidationHandler.class)).isTrue();
    }

    @Test
    void dp3_userLoaderHandler_extendsAuthHandler() {
        assertThat(AuthHandler.class.isAssignableFrom(UserLoaderHandler.class)).isTrue();
    }

    @Test
    void dp3_roleAuthorizationHandler_extendsAuthHandler() {
        assertThat(AuthHandler.class.isAssignableFrom(RoleAuthorizationHandler.class)).isTrue();
    }

    // =========================================================================
    // DP-4 Singleton — JwtConfigurationManager
    // =========================================================================

    @Test
    void dp4_jwtConfigurationManager_hasExactlyOnePrivateConstructor() {
        Constructor<?>[] constructors = JwtConfigurationManager.class.getDeclaredConstructors();
        assertThat(constructors).hasSize(1);
        assertThat(Modifier.isPrivate(constructors[0].getModifiers())).isTrue();
    }

    @Test
    void dp4_jwtConfigurationManager_getInstanceIsPublicAndStatic() throws Exception {
        Method m = JwtConfigurationManager.class.getMethod("getInstance");
        assertThat(Modifier.isPublic(m.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(m.getModifiers())).isTrue();
        assertThat(m.getReturnType()).isEqualTo(JwtConfigurationManager.class);
    }

    @Test
    void dp4_jwtConfigurationManager_sameInstanceReturnedTwice() {
        JwtConfigurationManager ref1 = JwtConfigurationManager.getInstance();
        JwtConfigurationManager ref2 = JwtConfigurationManager.getInstance();
        assertThat(ref1).isSameAs(ref2);
    }

    @Test
    void dp4_jwtConfigurationManager_threadSafe() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<JwtConfigurationManager>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                return JwtConfigurationManager.getInstance();
            }));
        }
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        JwtConfigurationManager first = futures.get(0).get();
        for (Future<JwtConfigurationManager> f : futures) {
            assertThat(f.get()).isSameAs(first);
        }
    }

    @Test
    void dp4_jwtConfigurationManager_isNotSpringBean() {
        Class<?> clazz = JwtConfigurationManager.class;
        assertThat(clazz.isAnnotationPresent(org.springframework.stereotype.Component.class)
                || clazz.isAnnotationPresent(org.springframework.stereotype.Service.class)
                || clazz.isAnnotationPresent(org.springframework.context.annotation.Configuration.class))
                .as("JwtConfigurationManager must NOT be a Spring-managed bean").isFalse();
    }

    // =========================================================================
    // DP-5 Factory Pattern
    // =========================================================================

    @Test
    void dp5_mongoEvent_isInterface_withRequiredMethods() throws Exception {
        assertThat(MongoEvent.class.isInterface()).isTrue();
        assertThat(MongoEvent.class.getMethod("getId")).isNotNull();
        assertThat(MongoEvent.class.getMethod("getTimestamp")).isNotNull();
        assertThat(MongoEvent.class.getMethod("getAction")).isNotNull();
        assertThat(MongoEvent.class.getMethod("getDetails")).isNotNull();
    }

    @Test
    void dp5_destinationEvent_implementsMongoEvent() {
        assertThat(MongoEvent.class.isAssignableFrom(DestinationEvent.class)).isTrue();
    }

    @Test
    void dp5_eventFactory_hasCreateEventMethod() throws Exception {
        Method m = EventFactory.class.getMethod("createEvent", EventType.class, Map.class);
        assertThat(m).isNotNull();
    }

    @Test
    void dp5_eventFactory_createEvent_destination_returnsDestinationEvent() {
        EventFactory factory = new EventFactory();
        Map<String, Object> params = new HashMap<>();
        params.put("destinationId", 42L);
        params.put("action", "INDEXED");
        MongoEvent event = factory.createEvent(EventType.DESTINATION, params);
        assertThat(event).isInstanceOf(DestinationEvent.class);
        DestinationEvent de = (DestinationEvent) event;
        assertThat(de.getDestinationId()).isEqualTo(42L);
        assertThat(de.getAction()).isEqualTo("INDEXED");
    }

    @Test
    void dp5_eventFactory_createEvent_nullParams_doesNotThrow() {
        EventFactory factory = new EventFactory();
        MongoEvent event = factory.createEvent(EventType.DESTINATION, null);
        assertThat(event).isInstanceOf(DestinationEvent.class);
    }

    @Test
    void dp5_eventFactory_createEvent_unsupportedType_throwsIllegalArgumentException() {
        EventFactory factory = new EventFactory();
        assertThatThrownBy(() -> factory.createEvent(EventType.AUTH, new HashMap<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dp5_eventFactory_createEvent_setsTimestamp() {
        EventFactory factory = new EventFactory();
        Map<String, Object> params = new HashMap<>();
        params.put("destinationId", 1L);
        MongoEvent event = factory.createEvent(EventType.DESTINATION, params);
        assertThat(event.getTimestamp()).isNotNull();
    }

    // =========================================================================
    // DP-6 Adapter Pattern
    // =========================================================================

    @Test
    void dp6_elasticsearchHitAdapter_adaptMethodExists_returnsCorrectType() throws Exception {
        Method m = ElasticsearchHitAdapter.class.getMethod("adapt", DestinationSearchDocument.class);
        assertThat(m.getReturnType()).isEqualTo(DestinationSearchResultDTO.class);
    }

    @Test
    void dp6_elasticsearchHitAdapter_convertsDocumentToDTO() {
        ElasticsearchHitAdapter adapter = new ElasticsearchHitAdapter();
        DestinationSearchDocument doc = new DestinationSearchDocument();
        doc.setId("10");
        doc.setName("Dahab");
        doc.setCountry("Egypt");
        doc.setCategory("ADVENTURE");
        doc.setDescription("Great diving");
        doc.setHighlights("Blue Hole Three Pools");
        doc.setRating(4.7);
        doc.setTotalRatings(3);
        doc.setStatus("ACTIVE");
        DestinationSearchResultDTO result = adapter.adapt(doc);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Dahab");
        assertThat(result.getCountry()).isEqualTo("Egypt");
        assertThat(result.getCategory()).isEqualTo("ADVENTURE");
        assertThat(result.getRating()).isEqualTo(4.7);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void dp6_elasticsearchHitAdapter_nullDoc_returnsNull() {
        assertThat(new ElasticsearchHitAdapter().adapt(null)).isNull();
    }

    @Test
    void dp6_mongoDocumentAdapter_adaptMethodExists_returnsMap() throws Exception {
        Method m = MongoDocumentAdapter.class.getMethod("adapt", DestinationEvent.class);
        assertThat(Map.class.isAssignableFrom(m.getReturnType())).isTrue();
    }

    @Test
    void dp6_mongoDocumentAdapter_convertsEventToMap() {
        MongoDocumentAdapter adapter = new MongoDocumentAdapter();
        DestinationEvent event = new DestinationEvent(1L, "INDEXED", LocalDateTime.now(), Map.of("k", "v"));
        Map<String, Object> result = adapter.adapt(event);
        assertThat(result).isNotNull();
        assertThat(result.get("destinationId")).isEqualTo(1L);
        assertThat(result.get("action")).isEqualTo("INDEXED");
        assertThat(result.get("timestamp")).isNotNull();
    }

    @Test
    void dp6_mongoDocumentAdapter_nullEvent_returnsNull() {
        assertThat(new MongoDocumentAdapter().adapt(null)).isNull();
    }

    @Test
    void dp6_objectArrayDtoAdapter_adaptRevenue_mapsAggregateToDto() {
        ObjectArrayDtoAdapter adapter = new ObjectArrayDtoAdapter();
        DestinationBookingRevenueAggregateDTO aggregate = new DestinationBookingRevenueAggregateDTO(
                5L, BigDecimal.valueOf(2000.0), BigDecimal.valueOf(400.0));
        DestinationRevenueDTO dto = adapter.adaptRevenue(1L, "Cairo", aggregate);
        assertThat(dto.getDestinationId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Cairo");
        assertThat(dto.getTotalBookings()).isEqualTo(5L);
        assertThat(dto.getTotalRevenue()).isEqualTo(2000.0);
        assertThat(dto.getAverageBookingAmount()).isEqualTo(400.0);
    }

    @Test
    void dp6_objectArrayDtoAdapter_nullAggregate_returnsZeroes() {
        ObjectArrayDtoAdapter adapter = new ObjectArrayDtoAdapter();
        DestinationRevenueDTO dto = adapter.adaptRevenue(2L, "Alexandria", (DestinationBookingRevenueAggregateDTO) null);
        assertThat(dto.getTotalBookings()).isEqualTo(0L);
        assertThat(dto.getTotalRevenue()).isEqualTo(0.0);
        assertThat(dto.getAverageBookingAmount()).isEqualTo(0.0);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static feign.FeignException.NotFound feignNotFound() {
        return new feign.FeignException.NotFound(
                "Not found",
                feign.Request.create(feign.Request.HttpMethod.GET, "/test", Map.of(), null, null, null),
                null,
                null);
    }

    private static Destination newDestination(Long id) {
        Destination d = new Destination();
        d.setId(id);
        d.setName("Test");
        d.setStatus(Destination.Status.ACTIVE);
        return d;
    }

    private static DestinationReview lowRatedReview(Long id, Destination destination, int rating) {
        DestinationReview r = new DestinationReview();
        r.setId(id);
        r.setDestination(destination);
        r.setRating(rating);
        return r;
    }
}