package com.randmteam2.tripplanning.itinerary.service;
import com.randmteam2.tripplanning.itinerary.dto.ItineraryAnalyticsDashboardDTO;

import com.randmteam2.tripplanning.itinerary.dto.*;
import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import com.randmteam2.tripplanning.itinerary.model.ItineraryDay;
import com.randmteam2.tripplanning.itinerary.mongo.ItineraryEventRepository;
import com.randmteam2.tripplanning.itinerary.observer.EntityObserver;
import com.randmteam2.tripplanning.itinerary.observer.MongoEventLogger;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryDayRepository;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final List<EntityObserver> observers = new CopyOnWriteArrayList<>();

    public ItineraryService(ItineraryRepository itineraryRepository,
                            ItineraryDayRepository itineraryDayRepository,
                            ItineraryEventRepository itineraryEventRepository) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        register(new MongoEventLogger(itineraryEventRepository));
    }

    public void register(EntityObserver observer) { observers.add(observer); }
    public void unregister(EntityObserver observer) { observers.remove(observer); }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    private Map<String, Object> itineraryPayload(String action, Itinerary itinerary) {
        Map<String, Object> map = new HashMap<>();
        map.put("action", action);
        map.put("itineraryId", itinerary.getId());
        map.put("userId", itinerary.getUserId());
        map.put("status", itinerary.getStatus() != null ? itinerary.getStatus().name() : null);
        return map;
    }

    public Itinerary create(Itinerary itinerary) {
        Itinerary saved = itineraryRepository.save(itinerary);
        notifyObservers("ITINERARY_CREATED", itineraryPayload("ITINERARY_CREATED", saved));
        return saved;
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
        Itinerary saved = itineraryRepository.save(existing);
        notifyObservers("ITINERARY_UPDATED", itineraryPayload("ITINERARY_UPDATED", saved));
        return saved;
    }

    public void delete(Long id) {
        Itinerary itinerary = getById(id);
        itineraryRepository.deleteById(id);
        notifyObservers("ITINERARY_DELETED", itineraryPayload("ITINERARY_DELETED", itinerary));
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

        Itinerary saved = itineraryRepository.save(itinerary);
        notifyObservers("ITINERARY_COMPLETED", itineraryPayload("ITINERARY_COMPLETED", saved));
        return saved;
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
        Itinerary saved = itineraryRepository.save(itinerary);
        notifyObservers("ITINERARY_CANCELLED", itineraryPayload("ITINERARY_CANCELLED", saved));
        return saved;
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
        Itinerary saved = itineraryRepository.save(itinerary);
        notifyObservers("DESTINATION_ASSIGNED", itineraryPayload("DESTINATION_ASSIGNED", saved));
        return saved;
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
        notifyObservers("DAYS_ADDED", itineraryPayload("DAYS_ADDED", result));
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
    public ItineraryAnalyticsDashboardDTO getAnalyticsDashboard(LocalDate startDate, LocalDate endDate) {

        // Log ANALYTICS_VIEWED on every call (even cache hits) — outside try block
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("itineraryId", 0L);
        eventPayload.put("startDate", startDate.toString());
        eventPayload.put("endDate", endDate.toString());
        notifyObservers("ANALYTICS_VIEWED", eventPayload);

        try {
            Object[] result = itineraryRepository.getDashboardAnalytics(startDate, endDate);

            Object[] row;
            if (result.length > 0 && result[0] instanceof Object[]) {
                row = (Object[]) result[0];
            } else {
                row = result;
            }

            long total = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            double totalBudget = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            double avgBudget = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            long completed = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            long cancelled = row[4] != null ? ((Number) row[4]).longValue() : 0L;
            long planned = row[5] != null ? ((Number) row[5]).longValue() : 0L;
            long draft = row[6] != null ? ((Number) row[6]).longValue() : 0L;
            long inProgress = row[7] != null ? ((Number) row[7]).longValue() : 0L;

            double completionRate = total > 0 ? (double) completed / total : 0.0;

            Map<String, Long> byStatus = new HashMap<>();
            if (completed > 0) byStatus.put("COMPLETED", completed);
            if (cancelled > 0) byStatus.put("CANCELLED", cancelled);
            if (planned > 0) byStatus.put("PLANNED", planned);
            if (draft > 0) byStatus.put("DRAFT", draft);
            if (inProgress > 0) byStatus.put("IN_PROGRESS", inProgress);

            return ItineraryAnalyticsDashboardDTO.builder()
                    .totalItineraries(total)
                    .totalBudget(totalBudget)
                    .averageBudget(avgBudget)
                    .completionRate(completionRate)
                    .itinerariesByStatus(byStatus)
                    .build();

        } catch (Exception e) {
            return ItineraryAnalyticsDashboardDTO.builder()
                    .totalItineraries(0)
                    .totalBudget(0.0)
                    .averageBudget(0.0)
                    .completionRate(0.0)
                    .itinerariesByStatus(new HashMap<>())
                    .build();
        }
    }
    public List<ItineraryDay> getDays(Long itineraryId) {
        getById(itineraryId);
        return itineraryDayRepository.findByItineraryIdOrderByDayOrder(itineraryId);
    }
}