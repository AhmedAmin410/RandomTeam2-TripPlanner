package com.randmteam2.tripplanning.activity.adapter;

import com.randmteam2.tripplanning.activity.dto.ActivityEventDTO;
import com.datastax.oss.driver.api.core.cql.Row;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * Adapter Pattern (DP-8): adapts a Cassandra Row to ActivityEventDTO.
 */
@Component
public class CassandraRowAdapter {

    public ActivityEventDTO adapt(Row row) {
        return ActivityEventDTO.builder()
                .eventId(row.getUuid("event_id"))
                .activityId(row.getLong("activityId"))
                .status(row.getString("status"))
                .timestamp(row.getInstant("timestamp"))
                .build();
    }
}
