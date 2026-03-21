package com.randmteam2.tripplanning.itinerary.service;

import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;

    public ItineraryService(ItineraryRepository itineraryRepository) {
        this.itineraryRepository = itineraryRepository;
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
}