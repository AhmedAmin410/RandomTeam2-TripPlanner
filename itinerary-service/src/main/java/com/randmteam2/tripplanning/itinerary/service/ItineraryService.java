package com.randmteam2.tripplanning.itinerary.service;

import com.randmteam2.tripplanning.itinerary.dto.ItineraryDayRequest;
import com.randmteam2.tripplanning.itinerary.dto.ItineraryDetailsDTO;
import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import com.randmteam2.tripplanning.itinerary.model.ItineraryDay;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryDayRepository;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryDayRepository itineraryDayRepository;

    public ItineraryService(ItineraryRepository itineraryRepository,
                            ItineraryDayRepository itineraryDayRepository) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryDayRepository = itineraryDayRepository;
    }


    public Itinerary create(Itinerary itinerary) {
        return itineraryRepository.save(itinerary);
    }

    public Itinerary getById(Long id) {
        return itineraryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Itinerary not found with id: " + id));
    }

    public List<Itinerary> getAll() {
        return itineraryRepository.findAll();
    }

    public Itinerary update(Long id, Itinerary updated) {
        Itinerary existing = getById(id);
        existing.setUserId(updated.getUserId());
        existing.setDestinationId(updated.getDestinationId());
        existing.setTitle(updated.getTitle());
        existing.setStatus(updated.getStatus());
        existing.setEstimatedBudget(updated.getEstimatedBudget());
        existing.setMetadata(updated.getMetadata());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        return itineraryRepository.save(existing);
    }
    @Transactional
    public Itinerary completeItinerary(Long id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Itinerary not found with id: " + id));

        if (itinerary.getStatus() != Itinerary.Status.IN_PROGRESS) {
            throw new RuntimeException("Itinerary must be IN_PROGRESS to complete it");
        }

        itinerary.setStatus(Itinerary.Status.COMPLETED);

        if (itinerary.getEstimatedBudget() == null) {
            Double total = itineraryRepository.sumConfirmedBookings(id);
            itinerary.setEstimatedBudget(total);
        }

        return itineraryRepository.save(itinerary);
    }
    public void delete(Long id) {
        getById(id);
        itineraryRepository.deleteById(id);
    }
    @Transactional
    public Itinerary cancelItinerary(Long id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Itinerary not found with id: " + id));

        if (itinerary.getStatus() != Itinerary.Status.DRAFT &&
                itinerary.getStatus() != Itinerary.Status.PLANNED) {
            throw new RuntimeException("Itinerary must be DRAFT or PLANNED to cancel it");
        }

        itinerary.setStatus(Itinerary.Status.CANCELLED);
        itineraryRepository.cancelPendingBookings(id);
        return itineraryRepository.save(itinerary);
    }
    @Transactional
    public Itinerary assignDestination(Long itineraryId, Long destinationId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found with id: " + itineraryId));

        if (itinerary.getStatus() != Itinerary.Status.DRAFT) {
            throw new RuntimeException("Itinerary must be DRAFT to assign a destination");
        }

        Integer exists = itineraryRepository.checkDestinationExists(destinationId);
        if (exists == 0) {
            throw new RuntimeException("Destination not found with id: " + destinationId);
        }

        Integer active = itineraryRepository.checkDestinationActive(destinationId);
        if (active == 0) {
            throw new RuntimeException("Destination must be ACTIVE to assign it");
        }

        itinerary.setDestinationId(destinationId);
        itinerary.setStatus(Itinerary.Status.PLANNED);
        return itineraryRepository.save(itinerary);
    }
    @Transactional
    public Itinerary addDays(Long itineraryId, List<ItineraryDayRequest> dayRequests) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found with id: " + itineraryId));

        if (itinerary.getStatus() != Itinerary.Status.DRAFT &&
                itinerary.getStatus() != Itinerary.Status.PLANNED) {
            throw new RuntimeException("cannot add days to itinerary with status: " + itinerary.getStatus());
        }

        for (ItineraryDayRequest req : dayRequests) {
            if (req.getDate() == null || req.getTitle() == null || req.getTitle().isBlank()) {
                throw new RuntimeException("each day must have a date and title");
            }
        }

        int maxOrder = itineraryRepository.getMaxDayOrder(itineraryId);

        List<ItineraryDay> newDays = new ArrayList<>();
        for (ItineraryDayRequest req : dayRequests) {
            maxOrder++;
            ItineraryDay day = new ItineraryDay();
            day.setDayOrder(maxOrder);
            day.setDate(req.getDate());
            day.setTitle(req.getTitle());
            day.setDescription(req.getDescription());
            day.setMetadata(req.getMetadata());
            day.setStatus(ItineraryDay.Status.PLANNED);
            day.setItinerary(itinerary);
            newDays.add(day);
        }

        itineraryDayRepository.saveAll(newDays);

        Itinerary result = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found with id: " + itineraryId));
        result.setItineraryDays(itineraryDayRepository.findByItineraryIdOrderByDayOrder(itineraryId));
        return result;
    }
    public ItineraryDetailsDTO getItineraryDetails(Long itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found with id: " + itineraryId));

        List<ItineraryDay> days = itineraryDayRepository.findByItineraryIdOrderByDayOrder(itineraryId);

        long completedDays = days.stream()
                .filter(d -> d.getStatus() == ItineraryDay.Status.COMPLETED)
                .count();

        ItineraryDetailsDTO dto = new ItineraryDetailsDTO();
        dto.setItineraryId(itinerary.getId());
        dto.setUserId(itinerary.getUserId());
        dto.setDestinationId(itinerary.getDestinationId());
        dto.setTitle(itinerary.getTitle());
        dto.setStatus(itinerary.getStatus().name());
        dto.setEstimatedBudget(itinerary.getEstimatedBudget());
        dto.setMetadata(itinerary.getMetadata());
        dto.setDays(days);
        dto.setTotalDays(days.size());
        dto.setCompletedDays(completedDays);

        return dto;
    }
}