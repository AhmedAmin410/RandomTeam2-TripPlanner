package com.randomteam2.tripplanning.destination;

import com.randomteam2.tripplanning.destination.observer.MongoEventLogger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.mockito.Mockito.mock;

@SpringBootTest
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class, MongoAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class, ElasticsearchRepositoriesAutoConfiguration.class, MongoRepositoriesAutoConfiguration.class})
class DestinationServiceApplicationTests {

    @Configuration
    static class TestConfig {

        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }

        @Bean
        public MongoEventLogger mongoEventLogger() {
            return mock(MongoEventLogger.class);
        }
    }

    @Test
    void contextLoads() {
    }

}