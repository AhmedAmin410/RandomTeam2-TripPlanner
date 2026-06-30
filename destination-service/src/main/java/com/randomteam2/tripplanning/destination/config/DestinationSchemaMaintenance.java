package com.randomteam2.tripplanning.destination.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DestinationSchemaMaintenance {

    @Bean
    ApplicationRunner relaxDestinationCategoryConstraint(JdbcTemplate jdbcTemplate) {
        return args -> jdbcTemplate.execute("""
                ALTER TABLE destinations DROP CONSTRAINT IF EXISTS destinations_category_check
                """);
    }
}
