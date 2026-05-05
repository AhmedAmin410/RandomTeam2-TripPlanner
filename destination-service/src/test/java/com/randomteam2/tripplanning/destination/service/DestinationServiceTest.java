package com.randomteam2.tripplanning.destination.service;

import com.randomteam2.tripplanning.destination.adapter.ElasticsearchHitAdapter;
import com.randomteam2.tripplanning.destination.adapter.MongoDocumentAdapter;
import com.randomteam2.tripplanning.destination.adapter.ObjectArrayDtoAdapter;
import com.randomteam2.tripplanning.destination.dto.DestinationDashboardDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationRateRequest;
import com.randomteam2.tripplanning.destination.dto.DestinationReviewAlertDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationRevenueDTO;
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
import com.randomteam2.tripplanning.destination.model.DestinationReviewType;
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
    @Mock private EntityObserver mockObserver;
    @InjectMocks private DestinationService destinationService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // =========================================================================
    // S2-F3: Revenue Summary
    // =========================================================================

    @Test
    void revenueSummary_mapsAggregateValuesToDto() {
        Destination destination = newDestination(1L);
        destination.setName("Cairo");
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.findDestinationRevenueSummary(1L, start, end))
                .thenReturn(new Object[]{5L, 2000.0, 400.0});
        when(objectArrayDtoAdapter.adaptRevenue(1L, "Cairo", new Object[]{5L, 2000.0, 400.0}))
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
        verify(destinationRepository).findDestinationRevenueSummary(1L, start, end);
    }

    @Test
    void revenueSummary_noBookings_returnsZeroes() {
        Destination destination = newDestination(2L);
        destination.setName("Alexandria");
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);
        when(destinationRepository.findById(2L)).thenReturn(Optional.of(destination));
        when(destinationRepository.findDestinationRevenueSummary(2L, start, end))
                .thenReturn(new Object[]{0L, 0.0, 0.0});
        when(objectArrayDtoAdapter.adaptRevenue(2L, "Alexandria", new Object[]{0L, 0.0, 0.0}))
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
        verify(destinationRepository, never()).findDestinationRevenueSummary(any(), any(), any());
    }

    @Test
    void revenueSummary_endDateBeforeStartDate_throws400() {
        LocalDate start = LocalDate.of(2026, 3, 31);
        LocalDate end = LocalDate.of(2026, 3, 1);
        assertThatThrownBy(() -> destinationService.getDestinationRevenueSummary(1L, start, end))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).findById(any());
        verify(destinationRepository, never()).findDestinationRevenueSummary(any(), any(), any());
    }

    @Test
    void revenueSummary_nullRow_returnsZeroes() {
        Destination destination = newDestination(1L);
        destination.setName("Cairo");
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        // No need to stub findDestinationRevenueSummary; service handles null by returning DTO with zeros
        when(objectArrayDtoAdapter.adaptRevenue(1L, "Cairo", null))
                .thenReturn(DestinationRevenueDTO.builder()
                        .destinationId(1L).name("Cairo")
                        .totalBookings(0L).totalRevenue(0.0).averageBookingAmount(0.0)
                        .build());
        DestinationRevenueDTO dto = destinationService.getDestinationRevenueSummary(1L, start, end);
        assertThat(dto.getTotalBookings()).isEqualTo(0L);
        assertThat(dto.getTotalRevenue()).isEqualTo(0.0);
        assertThat(dto.getAverageBookingAmount()).isEqualTo(0.0);
    }

    // =========================================================================
    // S2-F2: Update Details
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
    // S2-F4: Update Status
    // =========================================================================

    @Test
    void updateStatus_notFound_throws404() {
        when(destinationRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> destinationService.updateStatus(9L, "ACTIVE"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void updateStatus_invalidStatus_throws400() {
        assertThatThrownBy(() -> destinationService.updateStatus(1L, "RETIRED"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).findById(any());
    }

    @Test
    void updateStatus_blankStatus_throws400() {
        assertThatThrownBy(() -> destinationService.updateStatus(1L, "   "))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void updateStatus_inactiveWithActiveItineraries_throws400() {
        Destination dest = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(destinationRepository.countActiveItinerariesReferencingDestination(1L)).thenReturn(1L);
        assertThatThrownBy(() -> destinationService.updateStatus(1L, "INACTIVE"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verify(destinationRepository, never()).save(any());
    }

    @Test
    void updateStatus_active_noItineraryCheck_saves() {
        Destination dest = newDestination(2L);
        dest.setStatus(Destination.Status.INACTIVE);
        when(destinationRepository.findById(2L)).thenReturn(Optional.of(dest));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));
        Destination updated = destinationService.updateStatus(2L, "ACTIVE");
        assertThat(updated.getStatus()).isEqualTo(Destination.Status.ACTIVE);
        verify(destinationRepository, never()).countActiveItinerariesReferencingDestination(anyLong());
        verify(cacheInvalidationService).evictDestinationCaches(2L);
        verify(mongoEventLogger).onEvent(eq("STATUS_CHANGED"), any());
    }

    @Test
    void updateStatus_inactiveWhenNoActiveItineraries_saves() {
        Destination dest = newDestination(3L);
        when(destinationRepository.findById(3L)).thenReturn(Optional.of(dest));
        when(destinationRepository.countActiveItinerariesReferencingDestination(3L)).thenReturn(0L);
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));
        Destination updated = destinationService.updateStatus(3L, "inactive");
        assertThat(updated.getStatus()).isEqualTo(Destination.Status.INACTIVE);
        verify(cacheInvalidationService).evictDestinationCaches(3L);
        verify(mongoEventLogger).onEvent(eq("STATUS_CHANGED"), any());
    }

    @Test
    void updateStatus_seasonal_noItineraryCheck_saves() {
        Destination dest = newDestination(4L);
        when(destinationRepository.findById(4L)).thenReturn(Optional.of(dest));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));
        Destination updated = destinationService.updateStatus(4L, "SEASONAL");
        assertThat(updated.getStatus()).isEqualTo(Destination.Status.SEASONAL);
        verify(destinationRepository, never()).countActiveItinerariesReferencingDestination(anyLong());
        verify(mongoEventLogger).onEvent(eq("STATUS_CHANGED"), any());
    }

    // =========================================================================
    // S2-F5: Search By Details
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
    void searchByDetails_withStatus_passesNormalizedStatus() {
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
    // S2-F6: Top Rated Report
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
    // S2-F7: Rate After Visit
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
        verify(destinationRepository, never()).findItineraryDestinationIdAndStatus(any());
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
    void rateAfterVisit_itineraryNotFound_throws404() {
        Destination d = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        when(destinationRepository.findItineraryDestinationIdAndStatus(10L)).thenReturn(List.of());
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
        when(destinationRepository.findItineraryDestinationIdAndStatus(10L))
                .thenReturn(List.<Object[]>of(new Object[]{2L, "COMPLETED"}));
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
        when(destinationRepository.findItineraryDestinationIdAndStatus(10L))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "PLANNED"}));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(10L);
        req.setRating(5);
        assertThatThrownBy(() -> destinationService.rateAfterVisit(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void rateAfterVisit_firstRating_setsAverageAndCount() {
        Destination d = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        when(destinationRepository.findItineraryDestinationIdAndStatus(10L))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "COMPLETED"}));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(10L);
        req.setRating(5);
        Destination updated = destinationService.rateAfterVisit(1L, req);
        assertThat(updated.getRating()).isEqualTo(5.0);
        assertThat(updated.getTotalRatings()).isEqualTo(1);
        verify(cacheInvalidationService).evictDestinationCaches(1L);
        verify(mongoEventLogger).onEvent(eq("RATING_ADDED"), any());
    }

    @Test
    void rateAfterVisit_secondRating_recalculatesRunningAverage() {
        Destination d = newDestination(1L);
        d.setRating(5.0);
        d.setTotalRatings(1);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        when(destinationRepository.findItineraryDestinationIdAndStatus(11L))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "COMPLETED"}));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(11L);
        req.setRating(3);
        Destination updated = destinationService.rateAfterVisit(1L, req);
        assertThat(updated.getRating()).isEqualTo(4.0);
        assertThat(updated.getTotalRatings()).isEqualTo(2);
    }

    @Test
    void rateAfterVisit_ratingOne_isValidBoundary() {
        Destination d = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d));
        when(destinationRepository.findItineraryDestinationIdAndStatus(5L))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "COMPLETED"}));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));
        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(5L);
        req.setRating(1);
        Destination updated = destinationService.rateAfterVisit(1L, req);
        assertThat(updated.getRating()).isEqualTo(1.0);
    }

    // =========================================================================
    // S2-F8: Verify Review
    // =========================================================================

    @Test
    void verifyReview_destinationNotFound_throws404() {
        when(destinationRepository.findById(99L)).thenReturn(Optional.empty());
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        req.setVerifiedBy(3L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(99L, 10L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
        verify(destinationRepository, never()).countAdminUserById(anyLong());
    }

    @Test
    void verifyReview_missingVerifiedBy_throws400() {
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(newDestination(1L)));
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 10L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void verifyReview_nonAdmin_throws403() {
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(newDestination(1L)));
        when(destinationRepository.countAdminUserById(3L)).thenReturn(0L);
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        req.setVerifiedBy(3L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 10L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void verifyReview_reviewNotFound_throws404() {
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(newDestination(1L)));
        when(destinationRepository.countAdminUserById(3L)).thenReturn(1L);
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
        when(destinationRepository.countAdminUserById(3L)).thenReturn(1L);
        DestinationReview review = new DestinationReview();
        review.setId(10L);
        review.setDestination(d2);
        review.setVisitDate(LocalDate.now().minusDays(1));
        when(destinationReviewRepository.findById(10L)).thenReturn(Optional.of(review));
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        req.setVerifiedBy(3L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 10L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void verifyReview_futureVisit_throws400() {
        Destination d1 = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d1));
        when(destinationRepository.countAdminUserById(3L)).thenReturn(1L);
        DestinationReview review = new DestinationReview();
        review.setId(10L);
        review.setDestination(d1);
        review.setVisitDate(LocalDate.now().plusDays(1));
        when(destinationReviewRepository.findById(10L)).thenReturn(Optional.of(review));
        VerifyDestinationReviewRequest req = new VerifyDestinationReviewRequest();
        req.setVerifiedBy(3L);
        assertThatThrownBy(() -> destinationService.verifyDestinationReview(1L, 10L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void verifyReview_todayVisitDate_isNotFuture_succeeds() {
        Destination d1 = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d1));
        when(destinationRepository.countAdminUserById(3L)).thenReturn(1L);
        DestinationReview review = new DestinationReview();
        review.setId(20L);
        review.setType(ReviewType.VISITOR);
        review.setContent("Good");
        review.setRating(4);
        review.setDestination(d1);
        review.setVisitDate(LocalDate.now());
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

    @Test
    void verifyReview_success_setsVerifiedAndMetadata() {
        Destination d1 = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d1));
        when(destinationRepository.countAdminUserById(3L)).thenReturn(1L);
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

    // =========================================================================
    // S2-F9: Low Rated Reviews
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
    // Observer Pattern
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
    // S2-F12: Dashboard
    // =========================================================================

    @Test
    void dashboard_destinationNotFound_throws404() {
        when(destinationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> destinationService.getDestinationDashboard(999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void dashboard_returnsCorrectAggregation() {
        Destination dest = newDestination(1L);
        dest.setName("Luxor");
        dest.setRating(4.5);
        dest.setTotalRatings(2);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(destinationRepository.findDestinationDashboardStats(1L)).thenReturn(new Object[]{5L, 3L, 3L});
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
        when(destinationRepository.findDestinationDashboardStats(5L)).thenReturn(new Object[]{0L, 0L, 0L});
        DestinationDashboardDTO dto = destinationService.getDestinationDashboard(5L);
        assertThat(dto.getTotalItineraries()).isEqualTo(0L);
        assertThat(dto.getCompletedItineraries()).isEqualTo(0L);
        assertThat(dto.getTotalVisitors()).isEqualTo(0L);
    }

    @Test
    void dashboard_nullStats_returnsZeroes() {
        Destination dest = newDestination(3L);
        when(destinationRepository.findById(3L)).thenReturn(Optional.of(dest));
        when(destinationRepository.findDestinationDashboardStats(3L)).thenReturn(null);
        DestinationDashboardDTO dto = destinationService.getDestinationDashboard(3L);
        assertThat(dto.getTotalItineraries()).isEqualTo(0L);
    }

    @Test
    void dashboard_logsDashboardViewedOnEveryCall_includingCacheHits() {
        Destination dest = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(destinationRepository.findDestinationDashboardStats(1L)).thenReturn(new Object[]{0L, 0L, 0L});
        destinationService.getDestinationDashboard(1L);
        destinationService.getDestinationDashboard(1L);
        // DASHBOARD_VIEWED must be fired on every invocation, even if response was cached
        verify(mongoEventLogger, times(2)).onEvent(eq("DASHBOARD_VIEWED"), any());
    }

    // =========================================================================
    // DP-2 Observer — Reflection
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
    void dp5_eventFactory_createEvent_unsupportedType_throwsException() {
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
    void dp6_objectArrayDtoAdapter_adaptRevenue_mapsRowToDto() {
        ObjectArrayDtoAdapter adapter = new ObjectArrayDtoAdapter();
        DestinationRevenueDTO dto = adapter.adaptRevenue(1L, "Cairo", new Object[]{5L, 2000.0, 400.0});
        assertThat(dto.getDestinationId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Cairo");
        assertThat(dto.getTotalBookings()).isEqualTo(5L);
        assertThat(dto.getTotalRevenue()).isEqualTo(2000.0);
        assertThat(dto.getAverageBookingAmount()).isEqualTo(400.0);
    }

    @Test
    void dp6_objectArrayDtoAdapter_nullRow_returnsZeroes() {
        ObjectArrayDtoAdapter adapter = new ObjectArrayDtoAdapter();
        DestinationRevenueDTO dto = adapter.adaptRevenue(2L, "Alexandria", null);
        assertThat(dto.getTotalBookings()).isEqualTo(0L);
        assertThat(dto.getTotalRevenue()).isEqualTo(0.0);
        assertThat(dto.getAverageBookingAmount()).isEqualTo(0.0);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

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
