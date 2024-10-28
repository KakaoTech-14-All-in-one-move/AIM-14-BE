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

    public Mono<Boolean> enterServer(String userId, String serverId) {
        return hashOperations.putIfAbsent(getVoiceStateRedisKey(serverId), userId, VoiceState.of(userId, serverId));
    }

    public Mono<Boolean> updateVoiceState(String userId, VoiceState voiceState) {
        return hashOperations.put(getVoiceStateRedisKey(voiceState.getServerId()), userId, null);
    }

    private String getVoiceStateRedisKey(String serverId, String channelId) {
        return String.format("server:%s:channel:%s:users", serverId, channelId);
    }

    private String getVoiceStateRedisKey(String serverId) {
        return String.format("server:%s:channel", serverId);
    }
}
