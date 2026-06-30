package com.randomteam2.tripplanning.destination.config;

import com.randomteam2.tripplanning.destination.elasticsearch.DestinationSearchDocument;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

@Configuration
public class DestinationSearchIndexMaintenance {

    @Bean
    ApplicationRunner recreateDestinationSearchIndex(ElasticsearchOperations operations) {
        return args -> {
            IndexOperations index = operations.indexOps(DestinationSearchDocument.class);
            if (index.exists()) {
                index.delete();
            }
            index.createWithMapping();
        };
    }
}
