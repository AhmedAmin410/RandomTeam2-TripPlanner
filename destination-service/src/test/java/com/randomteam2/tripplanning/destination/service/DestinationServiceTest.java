package com.randomteam2.tripplanning.destination.service;

import com.randomteam2.tripplanning.destination.dto.DestinationRateRequest;
import com.randomteam2.tripplanning.destination.dto.DestinationReviewAlertDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationRevenueDTO;
import com.randomteam2.tripplanning.destination.dto.TopDestinationDTO;
import com.randomteam2.tripplanning.destination.dto.VerifyDestinationReviewRequest;
import com.randomteam2.tripplanning.destination.model.Destination;
import com.randomteam2.tripplanning.destination.model.DestinationReview;
import com.randomteam2.tripplanning.destination.model.DestinationReviewType;
import com.randomteam2.tripplanning.destination.observer.EntityObserver;
import com.randomteam2.tripplanning.destination.observer.MongoEventLogger;
import com.randomteam2.tripplanning.destination.repository.DestinationRepository;
import com.randomteam2.tripplanning.destination.repository.DestinationReviewRepository;
import com.randomteam2.tripplanning.destination.service.DestinationCacheInvalidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DestinationServiceTest {

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private DestinationReviewRepository destinationReviewRepository;

    @Mock
    private MongoEventLogger mongoEventLogger;

    @Mock
    private DestinationCacheInvalidationService cacheInvalidationService;

    @Mock
    private EntityObserver mockObserver;

    @InjectMocks
    private DestinationService destinationService;

    @Test
    void revenueSummary_mapsAggregateValuesToDto() {
        Destination destination = newDestination(1L);
        destination.setName("Cairo");

        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.findDestinationRevenueSummary(1L, start, end))
                .thenReturn(new Object[] { 5L, 2000.0, 400.0 });

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
                .thenReturn(new Object[] { 0L, 0.0, 0.0 });

        DestinationRevenueDTO dto = destinationService.getDestinationRevenueSummary(2L, start, end);

        assertThat(dto.getDestinationId()).isEqualTo(2L);
        assertThat(dto.getName()).isEqualTo("Alexandria");
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

        verify(destinationRepository).findById(99L);
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
        when(destinationRepository.findDestinationRevenueSummary(1L, start, end)).thenReturn(null);

        DestinationRevenueDTO dto = destinationService.getDestinationRevenueSummary(1L, start, end);

        assertThat(dto.getDestinationId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Cairo");
        assertThat(dto.getTotalBookings()).isEqualTo(0L);
        assertThat(dto.getTotalRevenue()).isEqualTo(0.0);
        assertThat(dto.getAverageBookingAmount()).isEqualTo(0.0);

        verify(destinationRepository).findById(1L);
        verify(destinationRepository).findDestinationRevenueSummary(1L, start, end);
    }



    // ... existing code ...
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
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Destination updated = destinationService.updateDetails(1L, incomingDetails);

        assertThat(updated.getDetails()).containsEntry("climate", "tropical");
        assertThat(updated.getDetails()).containsEntry("currency", "EUR");
        assertThat(updated.getDetails()).containsEntry("visaRequired", true);
        assertThat(updated.getDetails()).containsEntry("timezone", "GMT+2");

        verify(destinationRepository).findById(1L);
        verify(destinationRepository).save(destination);
        verify(cacheInvalidationService).evictDestinationCaches(1L);
        verify(mongoEventLogger).onEvent(eq("DETAILS_UPDATED"), any());
    }

    @Test
    void updateDetails_destinationNotFound_throws404() {
        when(destinationRepository.findById(99L)).thenReturn(Optional.empty());

        Map<String, Object> incomingDetails = new HashMap<>();
        incomingDetails.put("currency", "EUR");

        assertThatThrownBy(() -> destinationService.updateDetails(99L, incomingDetails))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));

        verify(destinationRepository).findById(99L);
        verify(destinationRepository, never()).save(any());
    }

    @Test
    void updateDetails_nullIncomingDetails_throws400() {
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(newDestination(1L)));

        assertThatThrownBy(() -> destinationService.updateDetails(1L, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));

        verify(destinationRepository).findById(1L);
        verify(destinationRepository, never()).save(any());
    }

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
        Destination dest = new Destination();
        dest.setId(1L);
        dest.setName("Paris");
        dest.setStatus(Destination.Status.ACTIVE);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(destinationRepository.countActiveItinerariesReferencingDestination(1L)).thenReturn(1L);

        assertThatThrownBy(() -> destinationService.updateStatus(1L, "INACTIVE"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));

        verify(destinationRepository, never()).save(any());
    }

    @Test
    void updateStatus_active_noItineraryCheck_saves() {
        Destination dest = new Destination();
        dest.setId(2L);
        dest.setName("Lyon");
        dest.setStatus(Destination.Status.INACTIVE);
        when(destinationRepository.findById(2L)).thenReturn(Optional.of(dest));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Destination updated = destinationService.updateStatus(2L, "ACTIVE");

        assertThat(updated.getStatus()).isEqualTo(Destination.Status.ACTIVE);
        verify(destinationRepository, never()).countActiveItinerariesReferencingDestination(anyLong());
        verify(destinationRepository).save(dest);
        verify(cacheInvalidationService).evictDestinationCaches(2L);
        verify(mongoEventLogger).onEvent(eq("STATUS_CHANGED"), any());
    }

    @Test
    void updateStatus_inactiveWhenNoActiveItineraries_saves() {
        Destination dest = new Destination();
        dest.setId(3L);
        dest.setName("Nice");
        dest.setStatus(Destination.Status.ACTIVE);
        when(destinationRepository.findById(3L)).thenReturn(Optional.of(dest));
        when(destinationRepository.countActiveItinerariesReferencingDestination(3L)).thenReturn(0L);
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Destination updated = destinationService.updateStatus(3L, "inactive");

        assertThat(updated.getStatus()).isEqualTo(Destination.Status.INACTIVE);
        verify(destinationRepository).save(dest);
        verify(cacheInvalidationService).evictDestinationCaches(3L);
        verify(mongoEventLogger).onEvent(eq("STATUS_CHANGED"), any());
    }

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
        when(destinationRepository.searchByDetailsKeyValue("climate", "tropical", null))
                .thenReturn(List.of());

        assertThat(destinationService.searchByDetailsKeyValue("climate", "tropical", null)).isEmpty();

        verify(destinationRepository).searchByDetailsKeyValue("climate", "tropical", null);
    }

    @Test
    void searchByDetails_withStatus_passesNormalizedStatus() {
        when(destinationRepository.searchByDetailsKeyValue("climate", "tropical", "ACTIVE"))
                .thenReturn(List.of());

        destinationService.searchByDetailsKeyValue("climate", "tropical", "active");

        verify(destinationRepository).searchByDetailsKeyValue(eq("climate"), eq("tropical"), eq("ACTIVE"));
    }

    @Test
    void topRatedReport_limitBelowOne_throws400() {
        assertThatThrownBy(() -> destinationService.getTopRatedDestinationsReport(0))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));

        verify(destinationRepository, never()).findTopRatedDestinationsReport(anyInt());
    }

    @Test
    void topRatedReport_mapsRowsToDto() {
        Object[] row = new Object[] { 10L, "Luxor", 4.9, 5L };
        when(destinationRepository.findTopRatedDestinationsReport(2)).thenReturn(List.<Object[]>of(row));

        List<TopDestinationDTO> list = destinationService.getTopRatedDestinationsReport(2);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getDestinationId()).isEqualTo(10L);
        assertThat(list.get(0).getName()).isEqualTo("Luxor");
        assertThat(list.get(0).getRating()).isEqualTo(4.9);
        assertThat(list.get(0).getTotalBookings()).isEqualTo(5L);
        verify(destinationRepository).findTopRatedDestinationsReport(2);
    }

    @Test
    void topRatedReport_nullRating_mapsToZero() {
        Object[] row = new Object[] { 1L, "X", null, 0L };
        when(destinationRepository.findTopRatedDestinationsReport(10)).thenReturn(List.<Object[]>of(row));

        TopDestinationDTO dto = destinationService.getTopRatedDestinationsReport(10).get(0);

        assertThat(dto.getRating()).isEqualTo(0.0);
    }

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

        verify(destinationRepository, never()).findItineraryDestinationIdAndStatus(any());
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
                .thenReturn(List.<Object[]>of(new Object[] { 2L, "COMPLETED" }));

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
                .thenReturn(List.<Object[]>of(new Object[] { 1L, "PLANNED" }));

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
                .thenReturn(List.<Object[]>of(new Object[] { 1L, "COMPLETED" }));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(10L);
        req.setRating(5);

        Destination updated = destinationService.rateAfterVisit(1L, req);

        assertThat(updated.getRating()).isEqualTo(5.0);
        assertThat(updated.getTotalRatings()).isEqualTo(1);
        verify(destinationRepository).save(any(Destination.class));
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
                .thenReturn(List.<Object[]>of(new Object[] { 1L, "COMPLETED" }));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DestinationRateRequest req = new DestinationRateRequest();
        req.setItineraryId(11L);
        req.setRating(3);

        Destination updated = destinationService.rateAfterVisit(1L, req);

        assertThat(updated.getRating()).isEqualTo(4.0);
        assertThat(updated.getTotalRatings()).isEqualTo(2);
        verify(destinationRepository).save(any(Destination.class));
        verify(cacheInvalidationService).evictDestinationCaches(1L);
        verify(mongoEventLogger).onEvent(eq("RATING_ADDED"), any());
    }

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
    void verifyReview_success_updatesReviewAndReturnsDestinationWithReviews() {
        Destination d1 = newDestination(1L);
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(d1));
        when(destinationRepository.countAdminUserById(3L)).thenReturn(1L);

        DestinationReview review = new DestinationReview();
        review.setId(10L);
        review.setType(DestinationReviewType.VISITOR);
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

        Destination result = destinationService.verifyDestinationReview(1L, 10L, req);

        ArgumentCaptor<DestinationReview> captor = ArgumentCaptor.forClass(DestinationReview.class);
        verify(destinationReviewRepository).save(captor.capture());
        assertThat(captor.getValue().getVerified()).isTrue();
        assertThat(captor.getValue().getMetadata()).containsKeys("verifiedAt", "verifiedBy");
        assertThat(captor.getValue().getMetadata().get("verifiedBy")).isEqualTo(3L);

        assertThat(result.getDestinationReviews()).hasSize(1);
        assertThat(result.getDestinationReviews().get(0).getVerified()).isTrue();
        verify(cacheInvalidationService).evictDestinationReviewCaches(1L, 10L);
        verify(mongoEventLogger).onEvent(eq("REVIEW_VERIFIED"), any());
    }

    @Test
    void register_addsObserver_andNotifies() {
        destinationService.register(mockObserver);

        Destination destination = newDestination(1L);
        Map<String, Object> existingDetails = new HashMap<>();
        existingDetails.put("climate", "tropical");
        destination.setDetails(existingDetails);
        Map<String, Object> incomingDetails = new HashMap<>();
        incomingDetails.put("currency", "EUR");
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        destinationService.updateDetails(1L, incomingDetails);

        verify(mockObserver).onEvent(eq("DETAILS_UPDATED"), any());
        verify(mongoEventLogger).onEvent(eq("DETAILS_UPDATED"), any());
    }

    @Test
    void register_nullObserver_ignored() {
        destinationService.register(null);

        Destination destination = newDestination(1L);
        Map<String, Object> existingDetails = new HashMap<>();
        existingDetails.put("climate", "tropical");
        destination.setDetails(existingDetails);
        Map<String, Object> incomingDetails = new HashMap<>();
        incomingDetails.put("currency", "EUR");
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        destinationService.updateDetails(1L, incomingDetails);

        verify(mockObserver, never()).onEvent(any(), any());
        verify(mongoEventLogger).onEvent(eq("DETAILS_UPDATED"), any());
    }

    @Test
    void register_duplicateObserver_notAddedTwice() {
        destinationService.register(mockObserver);
        destinationService.register(mockObserver); // second time

        Destination destination = newDestination(1L);
        Map<String, Object> existingDetails = new HashMap<>();
        existingDetails.put("climate", "tropical");
        destination.setDetails(existingDetails);
        Map<String, Object> incomingDetails = new HashMap<>();
        incomingDetails.put("currency", "EUR");
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        destinationService.updateDetails(1L, incomingDetails);

        verify(mockObserver).onEvent(eq("DETAILS_UPDATED"), any());
        verify(mongoEventLogger).onEvent(eq("DETAILS_UPDATED"), any());
    }

    @Test
    void unregister_removesObserver_noLongerNotifies() {
        destinationService.register(mockObserver);
        destinationService.unregister(mockObserver);

        Destination destination = newDestination(1L);
        Map<String, Object> existingDetails = new HashMap<>();
        existingDetails.put("climate", "tropical");
        destination.setDetails(existingDetails);
        Map<String, Object> incomingDetails = new HashMap<>();
        incomingDetails.put("currency", "EUR");
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        destinationService.updateDetails(1L, incomingDetails);

        verify(mockObserver, never()).onEvent(any(), any());
        verify(mongoEventLogger).onEvent(eq("DETAILS_UPDATED"), any());
    }

    @Test
    void unregister_notRegistered_doesNothing() {
        destinationService.unregister(mockObserver);

        Destination destination = newDestination(1L);
        Map<String, Object> existingDetails = new HashMap<>();
        existingDetails.put("climate", "tropical");
        destination.setDetails(existingDetails);
        Map<String, Object> incomingDetails = new HashMap<>();
        incomingDetails.put("currency", "EUR");
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        destinationService.updateDetails(1L, incomingDetails);

        verify(mockObserver, never()).onEvent(any(), any());
        verify(mongoEventLogger).onEvent(eq("DETAILS_UPDATED"), any());
    }

    private static Destination newDestination(long id) {
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

