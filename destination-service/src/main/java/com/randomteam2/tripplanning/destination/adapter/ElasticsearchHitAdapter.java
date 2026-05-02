package com.randomteam2.tripplanning.destination.adapter;

import com.randomteam2.tripplanning.destination.dto.DestinationSearchResultDTO;
import com.randomteam2.tripplanning.destination.elasticsearch.DestinationSearchDocument;
import org.springframework.stereotype.Component;

/**
 * Adapter Pattern: converts an Elasticsearch DestinationSearchDocument
 * (raw NoSQL result) into the domain DestinationSearchResultDTO.
 */
@Component
public class ElasticsearchHitAdapter {

    public DestinationSearchResultDTO adapt(DestinationSearchDocument doc) {
        if (doc == null) {
            return null;
        }
        return new DestinationSearchResultDTO(
                doc.getId() != null ? Long.parseLong(doc.getId()) : null,
                doc.getName(),
                doc.getCountry(),
                doc.getCategory(),
                doc.getDescription(),
                doc.getHighlights(),
                doc.getRating(),
                doc.getTotalRatings(),
                doc.getStatus()
        );
    }
}
