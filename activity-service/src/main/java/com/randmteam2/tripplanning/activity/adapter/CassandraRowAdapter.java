package com.randmteam2.tripplanning.activity.adapter;

import com.randmteam2.tripplanning.activity.dto.ActivityEventDTO;
import com.datastax.oss.driver.api.core.cql.Row;
import org.springframework.stereotype.Component;

/**
 * Adapter Pattern (DP-8): adapts a Cassandra Row to ActivityEventDTO.
 */
@Component
public class CassandraRowAdapter {

    public ActivityEventDTO adapt(Row row) {
        ActivityEventDTO dto = new ActivityEventDTO();
        dto.setTimestamp(row.getInstant("timestamp"));
        dto.setStatus(row.getString("status"));
        dto.setCategory(row.getString("category"));
        dto.setLatitude(row.getDouble("latitude"));
        dto.setLongitude(row.getDouble("longitude"));
        dto.setNotes(row.getString("notes"));
        return dto;
    }
}
