package com.randmteam2.tripplanning.itinerary.observer;

import com.randmteam2.tripplanning.itinerary.factory.EventFactory;
import com.randmteam2.tripplanning.itinerary.factory.EventType;
import com.randmteam2.tripplanning.itinerary.mongo.ItineraryEvent;
import com.randmteam2.tripplanning.itinerary.mongo.ItineraryEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    private final ItineraryEventRepository repository;

    public MongoEventLogger(ItineraryEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) payload;
            params.put("action", eventType);
            ItineraryEvent event = (ItineraryEvent) EventFactory.createEvent(EventType.ITINERARY, params);
            repository.save(event);
        } catch (Exception e) {
            log.warn("Failed to log itinerary event to MongoDB: {}", e.getMessage());
        }
    }
}
