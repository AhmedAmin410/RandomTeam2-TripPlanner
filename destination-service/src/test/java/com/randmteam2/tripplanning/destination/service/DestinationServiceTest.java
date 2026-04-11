package com.randmteam2.tripplanning.destination.service;

import com.randmteam2.tripplanning.destination.dto.TopDestinationDTO;
import com.randmteam2.tripplanning.destination.model.Destination;
import com.randmteam2.tripplanning.destination.repository.DestinationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DestinationServiceTest {

    @Mock
    private DestinationRepository destinationRepository;

    @InjectMocks
    private DestinationService destinationService;

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
    }

    @Test
    void searchByDetails_blankKey_throws400() {
        assertThatThrownBy(() -> destinationService.searchByDetailsKeyValue(" ", "tropical", null))
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
}
