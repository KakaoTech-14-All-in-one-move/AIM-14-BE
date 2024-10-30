package com.example.pitching.call.service;

import com.example.pitching.call.dto.VoiceState;
import com.example.pitching.call.exception.DuplicateOperationException;
import com.example.pitching.call.exception.ErrorCode;
import com.example.pitching.call.exception.WrongAccessException;
import com.example.pitching.call.operation.request.ChannelRequest;
import com.example.pitching.call.operation.request.StateRequest;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

@Component
public class VoiceStateManager {
    private final ReactiveHashOperations<String, String, String> hashOperations;
    private final ConvertService convertService;

    public VoiceStateManager(ReactiveStringRedisTemplate redisTemplate, ConvertService convertService) {
        hashOperations = redisTemplate.opsForHash();
        this.convertService = convertService;
    }

    public Mono<Boolean> addIfAbsentOrChangeChannel(ChannelRequest channelRequest, String userId, String jsonVoiceState) {
        return existsVoiceState(channelRequest.serverId(), userId)
                .flatMap(exist -> {
                    if (!exist) return addVoiceState(userId, channelRequest.serverId(), jsonVoiceState);
                    return changeChannelAndSave(channelRequest, userId);
                });
    }

    private Mono<Boolean> changeChannelAndSave(ChannelRequest channelRequest, String userId) {
        return getVoiceState(channelRequest.serverId(), userId)
                .flatMap(convertService::convertJsonToVoiceState)
                .flatMap(oldVoiceState -> changeChannelId(channelRequest, userId, oldVoiceState));
    }

    public Mono<Boolean> updateState(StateRequest stateRequest, String userId) {
        return existsVoiceState(stateRequest.serverId(), userId)
                .filter(Boolean.TRUE::equals)
                .then(updateAndGetVoiceState(stateRequest, userId))
                .switchIfEmpty(Mono.error(new WrongAccessException(ErrorCode.WRONG_ACCESS_INACTIVE_CHANNEL, stateRequest.channelId())));
    }

    private Mono<Boolean> updateAndGetVoiceState(StateRequest stateRequest, String userId) {
        return getVoiceState(stateRequest.serverId(), userId)
                .flatMap(convertService::convertJsonToVoiceState)
                .flatMap(oldVoiceState -> updateVoiceState(stateRequest, userId, oldVoiceState));
    }

    private Mono<Boolean> updateVoiceState(StateRequest stateRequest, String userId, VoiceState oldVoiceState) {
        return addVoiceState(userId, stateRequest.serverId(), convertService.convertObjectToJson(oldVoiceState.updateState(stateRequest)));
    }

    private Mono<Boolean> changeChannelId(ChannelRequest channelRequest, String userId, VoiceState oldVoiceState) {
        if (Objects.equals(oldVoiceState.getChannelId(), channelRequest.channelId()))
            return Mono.error(new DuplicateOperationException(ErrorCode.DUPLICATE_CHANNEL_ENTRY, channelRequest.channelId()));
        VoiceState newVoiceState = oldVoiceState.changeChannelId(channelRequest.channelId(), channelRequest.channelType());
        return addVoiceState(userId, channelRequest.serverId(), convertService.convertObjectToJson(newVoiceState));
    }

    public Mono<String> getVoiceState(String serverId, String userId) {
        return hashOperations.get(getVoiceStateRedisKey(serverId), userId);
    }

    public Mono<Long> removeVoiceState(String serverId, String userId) {
        return hashOperations.remove(getVoiceStateRedisKey(serverId), userId);
    }

    public Flux<Map.Entry<String, String>> getAllVoiceState(String serverId) {
        return hashOperations.entries(getVoiceStateRedisKey(serverId));
    }

    private Mono<Boolean> addVoiceState(String userId, String serverId, String jsonVoiceState) {
        return hashOperations.put(getVoiceStateRedisKey(serverId), userId, jsonVoiceState);
    }

    private Mono<Boolean> existsVoiceState(String serverId, String userId) {
        return hashOperations.hasKey(getVoiceStateRedisKey(serverId), userId);
    }

    private String getVoiceStateRedisKey(String serverId) {
        return String.format("server:%s:call", serverId);
    }
}
