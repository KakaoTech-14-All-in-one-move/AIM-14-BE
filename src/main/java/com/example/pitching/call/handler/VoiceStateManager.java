package com.example.pitching.call.handler;

import com.example.pitching.call.dto.VoiceState;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class VoiceStateManager {
    private final ReactiveHashOperations<String, String, VoiceState> hashOperations;

    public VoiceStateManager(ReactiveStringRedisTemplate redisTemplate) {
        hashOperations = redisTemplate.opsForHash();
    }

    public Mono<Boolean> enterChannel(String userId, String serverId, VoiceState voiceState) {
        return hashOperations.putIfAbsent(getVoiceStateRedisKey(serverId), userId, voiceState);
    }

    private String getVoiceStateRedisKey(String serverId) {
        return String.format("server:%s:call", serverId);
    }
}
