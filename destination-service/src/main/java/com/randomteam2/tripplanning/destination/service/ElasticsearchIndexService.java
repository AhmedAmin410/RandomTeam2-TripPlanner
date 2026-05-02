package com.randomteam2.tripplanning.destination.service;

import com.randomteam2.tripplanning.destination.elasticsearch.DestinationSearchDocument;
import com.randomteam2.tripplanning.destination.elasticsearch.DestinationSearchRepository;
import com.randomteam2.tripplanning.destination.model.Destination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ElasticsearchIndexService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchIndexService.class);

    private final DestinationSearchRepository searchRepository;

    public ElasticsearchIndexService(DestinationSearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    public void indexDestination(Destination destination, String source) {
        try {
            DestinationSearchDocument doc = toDocument(destination);
            searchRepository.save(doc);
            logger.info("Indexed destination id={} source={}", destination.getId(), source);
        } catch (Exception e) {
            logger.warn("Failed to index destination id={} in Elasticsearch", destination.getId(), e);
        }
    }

    public void deleteDestination(Long destinationId) {
        try {
            searchRepository.deleteById(String.valueOf(destinationId));
            logger.info("Removed destination id={} from Elasticsearch index", destinationId);
        } catch (Exception e) {
            logger.warn("Failed to delete destination id={} from Elasticsearch", destinationId, e);
        }
    }

    private DestinationSearchDocument toDocument(Destination destination) {
        DestinationSearchDocument doc = new DestinationSearchDocument();
        doc.setId(String.valueOf(destination.getId()));
        doc.setName(destination.getName());
        doc.setCountry(destination.getCountry());
        doc.setCategory(destination.getCategory() != null ? destination.getCategory().name() : null);
        doc.setDescription(destination.getDescription());
        doc.setRating(destination.getRating());
        doc.setTotalRatings(destination.getTotalRatings());
        doc.setStatus(destination.getStatus() != null ? destination.getStatus().name() : null);

        // Build highlights from details.topAttractions
        String highlights = "";
        if (destination.getDetails() != null) {
            Object topAttractions = destination.getDetails().get("topAttractions");
            if (topAttractions instanceof List<?> list) {
                highlights = list.stream()
                        .filter(item -> item != null)
                        .map(Object::toString)
                        .collect(Collectors.joining(" "));
            }
        }
        doc.setHighlights(highlights);

        return doc;
    }

    public DestinationSearchRepository getSearchRepository() {
        return searchRepository;
    }
}
