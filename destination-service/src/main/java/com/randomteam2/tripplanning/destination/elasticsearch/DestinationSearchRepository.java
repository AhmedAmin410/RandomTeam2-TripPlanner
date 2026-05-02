package com.randomteam2.tripplanning.destination.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DestinationSearchRepository extends ElasticsearchRepository<DestinationSearchDocument, String> {
}
