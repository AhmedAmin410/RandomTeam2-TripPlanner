package com.randmteam2.tripplanning.destination.service;

import com.randmteam2.tripplanning.destination.dto.TopDestinationDTO;
import com.randmteam2.tripplanning.destination.model.Destination;
import com.randmteam2.tripplanning.destination.repository.DestinationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

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

        return destinationRepository.searchByDetailsKeyValue(
                key.trim(),
                value.trim(),
                statusFilter);
    }

    @Transactional(readOnly = true)
    public List<TopDestinationDTO> getTopRatedDestinationsReport(int limit) {
        if (limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be at least 1");
        }

        List<Object[]> rows = destinationRepository.findTopRatedDestinationsReport(limit);
        List<TopDestinationDTO> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            TopDestinationDTO dto = new TopDestinationDTO();
            dto.setDestinationId(((Number) row[0]).longValue());
            dto.setName((String) row[1]);
            dto.setRating(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0);
            dto.setTotalBookings(((Number) row[3]).longValue());
            result.add(dto);
        }
        return result;
    }
}
