package com.example.pitching.call.service;

import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class VoiceStateManager {
    private final ReactiveHashOperations<String, String, String> hashOperations;

    public VoiceStateManager(ReactiveStringRedisTemplate redisTemplate) {
        hashOperations = redisTemplate.opsForHash();
    }

    public Mono<Boolean> addVoiceState(String userId, String serverId, String jsonVoiceState) {
        return hashOperations.putIfAbsent(getVoiceStateRedisKey(serverId), userId, jsonVoiceState);
    }

    public Mono<Long> removeVoiceState(String serverId, String userId) {
        return hashOperations.remove(getVoiceStateRedisKey(serverId), userId);
    }

    public Flux<Map.Entry<String, String>> getAllVoiceState(String serverId) {
        return hashOperations.entries(getVoiceStateRedisKey(serverId));
    }

    private String getVoiceStateRedisKey(String serverId) {
        return String.format("server:%s:call", serverId);
    }
}
