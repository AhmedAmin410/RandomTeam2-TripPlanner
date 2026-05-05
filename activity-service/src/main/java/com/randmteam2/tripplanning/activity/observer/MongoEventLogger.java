package com.randmteam2.tripplanning.activity.observer;
import com.randmteam2.tripplanning.activity.event.ActivityEvent;
import com.randmteam2.tripplanning.activity.event.EventFactory;
import com.randmteam2.tripplanning.activity.event.EventType;
import com.randmteam2.tripplanning.activity.repository.ActivityEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {
    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);
    private final ActivityEventRepository repo;
    private final EventType boundType = EventType.ACTIVITY;
    public MongoEventLogger(ActivityEventRepository repo) { this.repo = repo; }
    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            @SuppressWarnings("unchecked") Map<String,Object> data = (Map<String,Object>) payload;
            repo.save((ActivityEvent) EventFactory.createEvent(boundType, data));
        } catch (Exception e) { log.warn("MongoEventLogger failed: {}", e.getMessage()); }
    }
}
