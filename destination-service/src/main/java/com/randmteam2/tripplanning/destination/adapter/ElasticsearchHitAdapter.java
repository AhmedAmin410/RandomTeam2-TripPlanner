// This class implements the Adapter Pattern (DP-7) for Elasticsearch hits.
// It converts Elasticsearch SearchHit objects into DestinationDashboardDTO objects.
package com.randomteam2.tripplanning.destination.adapter;

import org.elasticsearch.search.SearchHit;
import com.randomteam2.tripplanning.destination.dto.DestinationDashboardDTO;

import java.util.Map;

public class ElasticsearchHitAdapter {

    public static DestinationDashboardDTO adapt(SearchHit hit) {
        if (hit == null || hit.getSourceAsMap() == null) {
            throw new IllegalArgumentException("SearchHit or its source map cannot be null");
        }

        Map<String, Object> source = hit.getSourceAsMap();

        return DestinationDashboardDTO.builder()
                .destinationId(((Number) source.get("destinationId")).longValue())
                .destinationName((String) source.get("destinationName"))
                .totalRevenue(((Number) source.get("totalRevenue")).doubleValue())
                .revenueBySeason((Map<String, Double>) source.get("revenueBySeason"))
                .build();
    }
}