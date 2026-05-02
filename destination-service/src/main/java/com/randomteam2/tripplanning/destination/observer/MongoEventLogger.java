package com.randomteam2.tripplanning.destination.observer;

import com.randomteam2.tripplanning.destination.mongo.DestinationEvent;
import com.randomteam2.tripplanning.destination.mongo.EventFactory;
import com.randomteam2.tripplanning.destination.mongo.EventType;
import com.randomteam2.tripplanning.destination.mongo.MongoEvent;
import com.randomteam2.tripplanning.destination.repository.DestinationEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {

    private static final Logger logger = LoggerFactory.getLogger(MongoEventLogger.class);

    private final EventFactory eventFactory;
    private final DestinationEventRepository destinationEventRepository;

    public MongoEventLogger(
            EventFactory eventFactory,
            DestinationEventRepository destinationEventRepository
    ) {
        this.eventFactory = eventFactory;
        this.destinationEventRepository = destinationEventRepository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> params = buildParams(eventType, payload);
            MongoEvent event = eventFactory.createEvent(EventType.DESTINATION, params);

            if (event instanceof DestinationEvent destinationEvent) {
                destinationEventRepository.save(destinationEvent);
            } else {
                logger.warn("EventFactory returned unsupported event type for destination-service: {}",
                        event.getClass().getName());
            }
        } catch (Exception exception) {
            logger.warn("Failed to write destination event to MongoDB. eventType={}, payload={}",
                    eventType, payload, exception);
        }
    }

    private Map<String, Object> buildParams(String eventType, Object payload) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", eventType);
        params.put("timestamp", LocalDateTime.now());

        if (payload instanceof Map<?, ?> payloadMap) {
            for (Map.Entry<?, ?> entry : payloadMap.entrySet()) {
                if (entry.getKey() != null) {
                    params.put(entry.getKey().toString(), entry.getValue());
                }
            }
        } else if (payload != null) {
            params.put("payload", payload);
        }

        return params;
    }
}