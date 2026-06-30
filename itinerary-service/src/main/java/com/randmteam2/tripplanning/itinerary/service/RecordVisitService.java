package com.randmteam2.tripplanning.itinerary.service;

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
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
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
    private final Driver neo4jDriver;
    private final List<EntityObserver> observers = new CopyOnWriteArrayList<>();

    public RecordVisitService(ItineraryRepository itineraryRepository,
                              UserNodeRepository userNodeRepository,
                              DestinationNodeRepository destinationNodeRepository,
                              ItineraryEventRepository itineraryEventRepository,
                              UserServiceClient userServiceClient,
                              DestinationServiceClient destinationServiceClient,
                              Driver neo4jDriver) {
        this.itineraryRepository = itineraryRepository;
        this.userNodeRepository = userNodeRepository;
        this.destinationNodeRepository = destinationNodeRepository;
        this.userServiceClient = userServiceClient;
        this.destinationServiceClient = destinationServiceClient;
        this.neo4jDriver = neo4jDriver;
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

        boolean createdOrUpdated = upsertVisitRelationship(
                userId, destinationId, itineraryId);

        // Log VISIT_RECORDED to MongoDB via Observer
        Map<String, Object> payload = new HashMap<>();
        payload.put("itineraryId", itineraryId);
        payload.put("userId", userId);
        payload.put("destinationId", destinationId);
        notifyObservers("VISIT_RECORDED", payload);

        return createdOrUpdated ? "Visit recorded successfully" : "Visit already recorded for this itinerary";
    }

    private boolean upsertVisitRelationship(Long userId,
                                            Long destinationId,
                                            Long itineraryId) {
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                    MERGE (u:User {userId: $userId})
                    MERGE (d:Destination {destinationId: $destinationId})
                    MERGE (u)-[r:VISITED]->(d)
                    WITH r, $itineraryId AS itineraryId,
                         coalesce(r.recorded_itinerary_ids, coalesce(r.recordedItineraryIds, [])) AS recordedIds
                    WITH r, itineraryId, recordedIds, itineraryId IN recordedIds AS alreadyRecorded
                    SET r.recordedItineraryIds = CASE
                            WHEN alreadyRecorded THEN recordedIds
                            ELSE recordedIds + itineraryId
                        END,
                        r.recorded_itinerary_ids = CASE
                            WHEN alreadyRecorded THEN recordedIds
                            ELSE recordedIds + itineraryId
                        END,
                        r.visitCount = CASE
                            WHEN alreadyRecorded THEN coalesce(r.visitCount, 0)
                            ELSE coalesce(r.visitCount, 0) + 1
                        END,
                        r.lastVisitDate = CASE
                            WHEN alreadyRecorded THEN r.lastVisitDate
                            ELSE localdatetime()
                        END
                    RETURN alreadyRecorded AS alreadyRecorded
                    """;
            var result = session.run(cypher, Values.parameters(
                    "userId", userId,
                    "destinationId", destinationId,
                    "itineraryId", itineraryId
            ));
            return result.hasNext() && !result.next().get("alreadyRecorded").asBoolean();
        }
    }
}
