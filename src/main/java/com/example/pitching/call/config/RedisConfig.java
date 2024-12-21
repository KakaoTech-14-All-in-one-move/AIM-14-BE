package com.example.pitching.call.config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

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

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory(RedisProperties redisProperties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.host());
        config.setPort(redisProperties.port());

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(LettuceConnectionFactory connectionFactory) {
        return new ReactiveStringRedisTemplate(connectionFactory);
    }

    @ConfigurationProperties("redis")
    public record RedisProperties(String host, int port, int maxlen) {
    }
}
