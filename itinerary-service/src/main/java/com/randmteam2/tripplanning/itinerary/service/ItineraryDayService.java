package com.randmteam2.tripplanning.itinerary.service;

import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import com.randmteam2.tripplanning.itinerary.model.ItineraryDay;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryDayRepository;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ItineraryDayService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryRepository itineraryRepository;

    public ItineraryDayService(ItineraryDayRepository itineraryDayRepository,
                               ItineraryRepository itineraryRepository) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.itineraryRepository = itineraryRepository;
    }


    public ItineraryDay create(Long itineraryId, ItineraryDay day) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found with id: " + itineraryId));
        day.setItinerary(itinerary);
        return itineraryDayRepository.save(day);
    }

    public ItineraryDay getById(Long id) {
        return itineraryDayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ItineraryDay not found with id: " + id));
    }

    public List<ItineraryDay> getAll() {
        return itineraryDayRepository.findAll();
    }

    public ItineraryDay update(Long id, ItineraryDay updated) {
        ItineraryDay existing = getById(id);
        existing.setDayOrder(updated.getDayOrder());
        existing.setDate(updated.getDate());
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setStatus(updated.getStatus());
        existing.setMetadata(updated.getMetadata());
        return itineraryDayRepository.save(existing);
    }

    public void delete(Long id) {
        getById(id);
        itineraryDayRepository.deleteById(id);
    }
}