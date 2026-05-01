package com.randmteam2.tripplanning.user.mongo;

import java.time.LocalDateTime;
import java.util.Map;

public interface MongoEvent {
    String getId();
    String getAction();
    LocalDateTime getTimestamp();
    Map<String, Object> getDetails();
}
