package com.randmteam2.tripplanning.itinerary.service;

import com.randmteam2.tripplanning.itinerary.dto.*;
import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import com.randmteam2.tripplanning.itinerary.model.ItineraryDay;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryDayRepository;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    @Transactional(noRollbackFor = RuntimeException.class)
    public Itinerary assignDestination(Long itineraryId, Long destinationId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found with id: " + itineraryId));

        if (itinerary.getStatus() != Itinerary.Status.DRAFT) {
            throw new RuntimeException("Itinerary must be DRAFT to assign a destination");
        }

        Integer exists = itineraryRepository.checkDestinationExists(destinationId);
        if (exists == null || exists == 0) {
            throw new RuntimeException("Destination not found with id: " + destinationId);
        }

        Integer active = itineraryRepository.checkDestinationActive(destinationId);
        if (active == null || active == 0) {
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
            if (req.date() == null || req.title() == null || req.title().isBlank()) {
                throw new RuntimeException("each day must have a date and title");
            }
        }

        int maxOrder = itineraryRepository.getMaxDayOrder(itineraryId);

        List<ItineraryDay> newDays = new ArrayList<>();
        for (ItineraryDayRequest req : dayRequests) {
            maxOrder++;
            ItineraryDay day = new ItineraryDay();
            day.setDayOrder(maxOrder);
            day.setDate(req.date());
            day.setTitle(req.title());
            day.setDescription(req.description());
            day.setMetadata(req.metadata());
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

        return ItineraryDetailsDTO.builder()
                .itineraryId(itinerary.getId())
                .userId(itinerary.getUserId())
                .destinationId(itinerary.getDestinationId())
                .title(itinerary.getTitle())
                .status(itinerary.getStatus().name())
                .estimatedBudget(itinerary.getEstimatedBudget())
                .metadata(itinerary.getMetadata())
                .days(days)
                .totalDays(days.size())
                .completedDays(completedDays)
                .build();
    }
    public List<Itinerary> searchByStatusAndDateRange(String status, LocalDate startDate, LocalDate endDate) {
        return itineraryRepository.searchByStatusAndDateRange(status, startDate, endDate);
    }

    public TripCostEstimateDTO estimateTripCost(TripCostRequestDTO request) {
        double accommodation = 150.0 * request.numberOfDays() * request.numberOfTravelers();
        double transport = 50.0 * request.numberOfDays() * request.numberOfTravelers();
        double activities = 100.0 * request.numberOfDays();

        Integer activeCount = itineraryRepository.countActiveItinerariesForDestination(request.destinationId());
        double seasonMultiplier;
        if (activeCount <= 5) {
            seasonMultiplier = 1.0;
        } else if (activeCount <= 15) {
            seasonMultiplier = 1.3;
        } else {
            seasonMultiplier = 1.6;
        }

        double total = (accommodation + transport + activities) * seasonMultiplier;

        return TripCostEstimateDTO.builder()
                .estimatedAccommodation(accommodation)
                .estimatedTransport(transport)
                .estimatedActivities(activities)
                .estimatedTotal(total)
                .seasonMultiplier(seasonMultiplier)
                .build();
    }
    public List<Itinerary> filterByMetadata(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new IllegalArgumentException("key and value must not be blank");
        }
        return itineraryRepository.filterByMetadata(key, value);
    }
    public ItineraryAnalyticsDTO getAnalytics(LocalDate startDate, LocalDate endDate) {
        try {
            Object[] result = itineraryRepository.getAnalytics(startDate, endDate);

            // Handle both Object[] and Object[][] cases
            Object[] row;
            if (result.length > 0 && result[0] instanceof Object[]) {
                row = (Object[]) result[0];
            } else {
                row = result;
            }

            long total = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            long completed = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            long cancelled = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            double totalBudget = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            double avgBudget = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            double completionRate = total > 0 ? (completed * 100.0) / total : 0.0;

            return ItineraryAnalyticsDTO.builder()
                    .totalItineraries(total)
                    .completedItineraries(completed)
                    .cancelledItineraries(cancelled)
                    .totalBudget(totalBudget)
                    .averageBudget(avgBudget)
                    .completionRate(completionRate)
                    .build();
        } catch (Exception e) {
            return ItineraryAnalyticsDTO.builder()
                    .totalItineraries(0L)
                    .completedItineraries(0L)
                    .cancelledItineraries(0L)
                    .totalBudget(0.0)
                    .averageBudget(0.0)
                    .completionRate(0.0)
                    .build();
        }
    }
    public List<ItineraryDay> getDays(Long itineraryId) {
        getById(itineraryId); // throws 404 if not found
        return itineraryDayRepository.findByItineraryIdOrderByDayOrder(itineraryId);
    }

}