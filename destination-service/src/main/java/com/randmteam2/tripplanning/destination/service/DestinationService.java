package com.randmteam2.tripplanning.destination.service;

import com.randmteam2.tripplanning.destination.model.Destination;
import com.randmteam2.tripplanning.destination.repository.DestinationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DestinationService {

    private final DestinationRepository destinationRepository;

    public DestinationService(DestinationRepository destinationRepository) {
        this.destinationRepository = destinationRepository;
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
            long activeRefs = destinationRepository.countActiveItinerariesReferencingDestination(id);
            if (activeRefs > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Active itineraries still reference this destination");
            }
        }

        destination.setStatus(newStatus);
        return destinationRepository.save(destination);
    }
}
