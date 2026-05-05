package com.randmteam2.tripplanning.activity.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("activity-service::activity",  defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("activity-service::S4-F1",     defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("activity-service::S4-F3",     defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("activity-service::S4-F5",     defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("activity-service::S4-F6",     defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("activity-service::S4-F8",     defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("activity-service::S4-F9",     defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("activity-service::S4-F10",    defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("activity-service::S4-F12",    defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
