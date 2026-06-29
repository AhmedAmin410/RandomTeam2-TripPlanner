package com.randmteam2.tripplanning.itinerary.service;

import com.randmteam2.tripplanning.itinerary.dto.DestinationDTO;
import com.randmteam2.tripplanning.itinerary.dto.UserDTO;
import com.randmteam2.tripplanning.itinerary.feign.DestinationServiceClient;
import com.randmteam2.tripplanning.itinerary.feign.UserServiceClient;
import com.randmteam2.tripplanning.itinerary.model.Itinerary;
import com.randmteam2.tripplanning.itinerary.mongo.ItineraryEventRepository;
import com.randmteam2.tripplanning.itinerary.neo4j.DestinationNode;
import com.randmteam2.tripplanning.itinerary.neo4j.DestinationNodeRepository;
import com.randmteam2.tripplanning.itinerary.neo4j.UserNode;
import com.randmteam2.tripplanning.itinerary.neo4j.UserNodeRepository;
import com.randmteam2.tripplanning.itinerary.neo4j.VisitedRelationship;
import com.randmteam2.tripplanning.itinerary.observer.EntityObserver;
import com.randmteam2.tripplanning.itinerary.observer.MongoEventLogger;
import com.randmteam2.tripplanning.itinerary.repository.ItineraryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RecordVisitService {

    private final ItineraryRepository itineraryRepository;
    private final UserNodeRepository userNodeRepository;
    private final DestinationNodeRepository destinationNodeRepository;
    private final UserServiceClient userServiceClient;
    private final DestinationServiceClient destinationServiceClient;
    private final List<EntityObserver> observers = new CopyOnWriteArrayList<>();

    public RecordVisitService(ItineraryRepository itineraryRepository,
                              UserNodeRepository userNodeRepository,
                              DestinationNodeRepository destinationNodeRepository,
                              ItineraryEventRepository itineraryEventRepository,
                              UserServiceClient userServiceClient,
                              DestinationServiceClient destinationServiceClient) {
        this.itineraryRepository = itineraryRepository;
        this.userNodeRepository = userNodeRepository;
        this.destinationNodeRepository = destinationNodeRepository;
        this.userServiceClient = userServiceClient;
        this.destinationServiceClient = destinationServiceClient;
        register(new MongoEventLogger(itineraryEventRepository));
    }

    public void register(EntityObserver observer) { observers.add(observer); }
    public void unregister(EntityObserver observer) { observers.remove(observer); }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    public String recordVisit(Long itineraryId) {
        // Find itinerary
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found with id: " + itineraryId));

        // Validate status is COMPLETED
        if (itinerary.getStatus() != Itinerary.Status.COMPLETED) {
            throw new IllegalArgumentException("Itinerary must be COMPLETED to record a visit");
        }

        // Validate destinationId is not null
        if (itinerary.getDestinationId() == null) {
            throw new IllegalArgumentException("Itinerary must have a destination assigned");
        }

        Long userId = itinerary.getUserId();
        Long destinationId = itinerary.getDestinationId();

        UserDTO user = userServiceClient.getUser(userId);
        String userName = valueAsString(user != null ? user.getName() : null, "Unknown");

        DestinationDTO destination = destinationServiceClient.getDestination(destinationId);
        String destName = valueAsString(destination != null ? destination.getName() : null, "Unknown");
        String destCountry = valueAsString(destination != null ? destination.getCountry() : null, "");
        String destCategory = valueAsString(destination != null ? destination.getCategory() : null, "");

        // Find or create UserNode
        UserNode userNode = userNodeRepository.findByUserId(userId)
                .orElse(new UserNode(userId, userName));

        // Find or create DestinationNode
        DestinationNode destinationNode = destinationNodeRepository.findByDestinationId(destinationId)
                .orElse(new DestinationNode(destinationId, destName, destCountry, destCategory));
        destinationNodeRepository.save(destinationNode);

        // Check idempotency
        VisitedRelationship existingRel = userNode.getVisited().stream()
                .filter(v -> v.getDestination().getDestinationId().equals(destinationId))
                .findFirst()
                .orElse(null);

        if (existingRel != null) {
            // Check if this itinerary was already recorded
            if (existingRel.getRecordedItineraryIds().contains(itineraryId)) {
                return "Visit already recorded for this itinerary";
            }
            // Increment visitCount
            existingRel.setVisitCount(existingRel.getVisitCount() + 1);
            existingRel.setLastVisitDate(LocalDateTime.now());
            existingRel.getRecordedItineraryIds().add(itineraryId);
        } else {
            // Create new VISITED relationship
            VisitedRelationship rel = new VisitedRelationship(destinationNode);
            rel.setVisitCount(1);
            rel.setLastVisitDate(LocalDateTime.now());
            rel.getRecordedItineraryIds().add(itineraryId);
            userNode.getVisited().add(rel);
        }

        userNodeRepository.save(userNode);

        // Log VISIT_RECORDED to MongoDB via Observer
        Map<String, Object> payload = new HashMap<>();
        payload.put("itineraryId", itineraryId);
        payload.put("userId", userId);
        payload.put("destinationId", destinationId);
        notifyObservers("VISIT_RECORDED", payload);

        return "Visit recorded successfully";
    }

    private String valueAsString(Object value, String fallback) {
        return value != null ? value.toString() : fallback;
    }
}
