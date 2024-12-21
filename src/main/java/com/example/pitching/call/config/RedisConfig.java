package com.example.pitching.call.config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RedisConfig {
    @Bean
    public RedisClient redisClient(RedisProperties redisProperties) {
        log.info("Redis Connection : {}:{}", redisProperties.host, redisProperties.port);
        return RedisClient.create(RedisURI.builder()
                .withHost(redisProperties.host)
                .withPort(redisProperties.port)
                .build());
    }

    @Bean
    public StatefulRedisConnection<String, String> statefulRedisConnection(RedisClient redisClient) {
        return redisClient.connect();
    }

    @Bean
    public RedisReactiveCommands<String, String> redisCommands(StatefulRedisConnection<String, String> connection) {
        return connection.reactive();
    }

    @ConfigurationProperties("redis")
    public record RedisProperties(String host, int port, int maxlen) {
    }
}
