// This class implements the Adapter Pattern (DP-7) for Cassandra rows.
// It converts Cassandra Row objects into ActivityEventDTO objects.

package com.randmteam2.tripplanning.activity.adapter;

import com.datastax.oss.driver.api.core.cql.Row;
import com.randmteam2.tripplanning.activity.dto.ActivityEventDTO;

public class CassandraRowAdapter {

    public static ActivityEventDTO adapt(Row row) {
        if (row == null) {
            throw new IllegalArgumentException("Row cannot be null");
        }

        return ActivityEventDTO.builder()
                .eventId(row.getUuid("event_id"))
                .activityId(row.getLong("activity_id"))
                .status(row.getString("status"))
                .timestamp(row.getInstant("timestamp"))
                .build();
    }
}