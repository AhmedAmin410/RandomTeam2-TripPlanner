package com.randmteam2.tripplanning.destination.service;

import com.randmteam2.tripplanning.destination.model.Destination;
import com.randmteam2.tripplanning.destination.repository.DestinationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
