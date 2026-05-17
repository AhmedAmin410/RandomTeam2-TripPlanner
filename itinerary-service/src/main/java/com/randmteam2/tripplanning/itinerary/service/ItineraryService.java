package com.randmteam2.tripplanning.itinerary.service;
import com.randmteam2.tripplanning.itinerary.dto.ItineraryAnalyticsDashboardDTO;

import com.randmteam2.tripplanning.itinerary.dto.*;
import com.randmteam2.tripplanning.itinerary.feign.BookingServiceClient;
import com.randmteam2.tripplanning.itinerary.feign.DestinationServiceClient;
import com.randmteam2.tripplanning.itinerary.feign.UserServiceClient;
import com.randmteam2.tripplanning.itinerary.messaging.ItineraryEventPublisher;
import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import com.randmteam2.tripplanning.itinerary.model.ItineraryDay;
import com.randmteam2.tripplanning.itinerary.mongo.ItineraryEventRepository;
import com.randmteam2.tripplanning.itinerary.observer.EntityObserver;
import com.randmteam2.tripplanning.itinerary.observer.MongoEventLogger;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryDayRepository;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryRepository;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ItineraryService {
    private final ItineraryEventPublisher itineraryEventPublisher;

    private final UserServiceClient userServiceClient;
    private final DestinationServiceClient destinationServiceClient;
    private final BookingServiceClient bookingServiceClient;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final List<EntityObserver> observers = new CopyOnWriteArrayList<>();

    public ItineraryService(ItineraryEventPublisher itineraryEventPublisher, ItineraryRepository itineraryRepository,
                            ItineraryDayRepository itineraryDayRepository,
                            ItineraryEventRepository itineraryEventRepository, UserServiceClient userServiceClient, DestinationServiceClient destinationServiceClient, BookingServiceClient bookingServiceClient) {
        this.itineraryEventPublisher = itineraryEventPublisher;
        this.itineraryRepository = itineraryRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.userServiceClient = userServiceClient;
        this.destinationServiceClient = destinationServiceClient;
        this.bookingServiceClient = bookingServiceClient;
        register(new MongoEventLogger(itineraryEventRepository));
    }

    public void register(EntityObserver observer) { observers.add(observer); }
    public void unregister(EntityObserver observer) { observers.remove(observer); }
    /**
     * MOD-IT1: Observer retrofit on M1 itinerary write endpoints.
     * Called after every state-changing operation (create, update, delete,
     * cancel, complete, assignDestination, addDays) to log events to MongoDB
     * via the registered EntityObserver chain.
     */
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
    private static final Set<Itinerary.Status> STATUS_COMPLETED_FAMILY = Set.of(
            Itinerary.Status.COMPLETED,
            Itinerary.Status.COMPLETING,
            Itinerary.Status.PAYMENT_PENDING,
            Itinerary.Status.PAID
    );

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

        // Feign pre-check 1: user must be ACTIVE
        try {
            UserDTO user = userServiceClient.getUser(itinerary.getUserId());
            if (!"ACTIVE".equals(user.getStatus())) {
                throw new RuntimeException("User is not active");
            }
        } catch (feign.FeignException.NotFound e) {
            throw new RuntimeException("User not found");
        }

        // Feign pre-check 2: destination must be ACTIVE
        try {
            DestinationDTO destination = destinationServiceClient.getDestination(itinerary.getDestinationId());
            if (!"ACTIVE".equals(destination.getStatus())) {
                throw new RuntimeException("Destination is not active");
            }
        } catch (feign.FeignException.NotFound e) {
            throw new RuntimeException("Destination not found");
        }

        // Feign pre-check 3: must have at least 1 confirmed booking
        BookingConfirmedSummaryDTO summary;
        try {
            summary = bookingServiceClient.getConfirmedSummary(id);
        } catch (feign.FeignException e) {
            throw new RuntimeException("Booking service unavailable");
        }

        if (summary.count() < 1) {
            throw new RuntimeException("Itinerary has no CONFIRMED bookings");
        }

        // atomic UPDATE — only one concurrent caller wins
        int updated = itineraryRepository.atomicTransition(
                id, Itinerary.Status.COMPLETING, Itinerary.Status.IN_PROGRESS
        );
        if (updated == 0) {
            throw new RuntimeException("Itinerary completion already in progress");
        }

        // save budget
        itinerary.setEstimatedBudget(summary.totalRevenue());
        itineraryRepository.save(itinerary);

        notifyObservers("ITINERARY_COMPLETING", itineraryPayload("ITINERARY_COMPLETING", itinerary));
        itineraryEventPublisher.publishItineraryCompleted(
                itinerary.getId(),
                itinerary.getUserId(),
                itinerary.getDestinationId(),
                summary.totalRevenue()
        );
        return itineraryRepository.findById(id).get();
    }

    @Transactional
    public Itinerary cancelItinerary(Long id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Itinerary not found with id: " + id));

        if (itinerary.getStatus() != Itinerary.Status.DRAFT &&
                itinerary.getStatus() != Itinerary.Status.PLANNED) {
            throw new RuntimeException("Itinerary must be DRAFT or PLANNED to cancel it");
        }

        Itinerary saved = itineraryRepository.save(itinerary);
        notifyObservers("ITINERARY_CANCELLED", itineraryPayload("ITINERARY_CANCELLED", saved));
        itineraryEventPublisher.publishItineraryCancelled(
                saved.getId(), saved.getUserId(), saved.getDestinationId(), "user_requested"
        );
        return saved;
    }
    @Transactional
    public Itinerary assignDestination(Long itineraryId, Long destinationId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found with id: " + itineraryId));

        if (itinerary.getStatus() != Itinerary.Status.DRAFT) {
            throw new RuntimeException("Itinerary must be DRAFT to assign a destination");
        }

        DestinationDTO destination;
        try {
            destination = destinationServiceClient.getDestination(destinationId);
        } catch (feign.FeignException.NotFound e) {
            throw new RuntimeException("Destination not found with id: " + destinationId);
        } catch (feign.FeignException e) {
            throw new RuntimeException("Destination service unavailable");
        }

        if (!"ACTIVE".equals(destination.getStatus())) {
            throw new RuntimeException("Destination is not active");
        }

        Itinerary saved = itineraryRepository.save(itinerary);
        notifyObservers("DESTINATION_ASSIGNED", itineraryPayload("DESTINATION_ASSIGNED", saved));
        itineraryEventPublisher.publishItineraryPlaced(
                saved.getId(), saved.getUserId(), saved.getDestinationId()
        );
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

        // Log ANALYTICS_VIEWED on every call (even cache hits) Ã¢â‚¬â€ outside try block
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


    public UserTripSummaryAggregateDTO getUserTripSummary(Long userId) {
        List<Itinerary> all = itineraryRepository.findByUserId(userId);

        long total = all.size();
        long completed = all.stream()
                .filter(i -> STATUS_COMPLETED_FAMILY.contains(i.getStatus()))
                .count();
        long cancelled = all.stream()
                .filter(i -> i.getStatus() == Itinerary.Status.CANCELLED)
                .count();
        Double totalBudget = all.stream()
                .filter(i -> STATUS_COMPLETED_FAMILY.contains(i.getStatus()))
                .mapToDouble(i -> i.getEstimatedBudget() != null ? i.getEstimatedBudget() : 0.0)
                .sum();
        Double avgBudget = completed > 0 ? totalBudget / completed : 0.0;

        return new UserTripSummaryAggregateDTO(total, completed, cancelled, totalBudget, avgBudget);
    }
    public int getUserActiveCount(Long userId) {
        Set<Itinerary.Status> activeStatuses = Set.of(
                Itinerary.Status.DRAFT,
                Itinerary.Status.PLANNED,
                Itinerary.Status.IN_PROGRESS,
                Itinerary.Status.COMPLETING,
                Itinerary.Status.PAYMENT_PENDING
        );
        return (int) itineraryRepository.findByUserId(userId).stream()
                .filter(i -> activeStatuses.contains(i.getStatus()))
                .count();
    }

    public long getUserCompletedCount(Long userId) {
        return itineraryRepository.findByUserId(userId).stream()
                .filter(i -> STATUS_COMPLETED_FAMILY.contains(i.getStatus()))
                .count();
    }

    public int getDestinationActiveCount(Long destinationId) {
        Set<Itinerary.Status> activeStatuses = Set.of(
                Itinerary.Status.DRAFT,
                Itinerary.Status.PLANNED,
                Itinerary.Status.IN_PROGRESS,
                Itinerary.Status.COMPLETING,
                Itinerary.Status.PAYMENT_PENDING
        );
        return (int) itineraryRepository.findByDestinationId(destinationId).stream()
                .filter(i -> activeStatuses.contains(i.getStatus()))
                .count();
    }

    public DestinationBookingRevenueAggregateDTO getDestinationBookingRevenue(Long destinationId) {
        try {
            List<Itinerary> itineraries = itineraryRepository.findByDestinationId(destinationId);
            List<Long> ids = itineraries.stream().map(Itinerary::getId).toList();
            if (ids.isEmpty()) {
                return new DestinationBookingRevenueAggregateDTO(0, 0.0, 0.0);
            }
            List<BookingAggregateDTO> bookings = bookingServiceClient.aggregateByItineraries(ids);
            long totalBookings = bookings.stream()
                    .mapToLong(b -> b.getTotalBookings() != null ? b.getTotalBookings() : 0L)
                    .sum();
            double totalRevenue = bookings.stream()
                    .mapToDouble(b -> b.getTotalRevenue() != null ? b.getTotalRevenue() : 0.0)
                    .sum();
            double avg = totalBookings > 0 ? totalRevenue / totalBookings : 0.0;
            return new DestinationBookingRevenueAggregateDTO(totalBookings, totalRevenue, avg);
        } catch (Exception e) {
            return new DestinationBookingRevenueAggregateDTO(0, 0.0, 0.0);
        }
    }

    public DestinationDashboardAggregateDTO getDestinationDashboardAggregate(Long destinationId) {
        List<Itinerary> itineraries = itineraryRepository.findByDestinationId(destinationId);
        long total = itineraries.size();
        long completed = itineraries.stream()
                .filter(i -> STATUS_COMPLETED_FAMILY.contains(i.getStatus()))
                .count();
        long visitors = itineraries.stream()
                .map(Itinerary::getUserId)
                .distinct()
                .count();
        return new DestinationDashboardAggregateDTO(total, completed, visitors);
    }

    public List<ItinerarySummaryDTO> getBatch(List<Long> itineraryIds) {
        return itineraryRepository.findAllById(itineraryIds).stream()
                .map(i -> new ItinerarySummaryDTO(
                        i.getId(), i.getDestinationId(), i.getUserId(), i.getStatus().name()
                ))
                .toList();
    }
}
