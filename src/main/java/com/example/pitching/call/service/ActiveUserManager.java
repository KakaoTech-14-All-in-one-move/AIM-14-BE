package com.example.pitching.call.service;

import com.example.pitching.call.exception.DuplicateOperationException;
import com.example.pitching.call.exception.ErrorCode;
import com.example.pitching.call.exception.WrongAccessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Component
public class ActiveUserManager {
    private final ReactiveValueOperations<String, String> valueOperations;

    public ActiveUserManager(ReactiveStringRedisTemplate redisTemplate) {
        this.valueOperations = redisTemplate.opsForValue();
    }

    // 서버 입장
    public Mono<Boolean> addActiveUser(String userId, String serverId) {
        return isActiveUser(userId)
                .flatMap(isActive -> {
                    if (!isActive) return valueOperations.setIfAbsent(getActiveUserRedisKey(userId), serverId);
                    return valueOperations.getAndSet(getActiveUserRedisKey(userId), serverId)
                            .flatMap(pastServerId -> validateServerDestination(serverId, pastServerId));
                });
    }

    // 로그아웃
    public Mono<String> removeActiveUser(String userId) {
        return valueOperations.getAndDelete(getActiveUserRedisKey(userId));
    }

    // 채널 입장
    public Mono<String> isCorrectAccess(String userId, String serverId) {
        return valueOperations.get(getActiveUserRedisKey(userId))
                .filter(currentServerId -> Objects.equals(currentServerId, serverId))
                .switchIfEmpty(Mono.error(new WrongAccessException(ErrorCode.WRONG_ACCESS_INACTIVE_SERVER, serverId)));
    }

    private Mono<Boolean> validateServerDestination(String serverId, String pastServerId) {
        if (!Objects.equals(pastServerId, serverId)) return Mono.just(true);
        return Mono.error(new DuplicateOperationException(ErrorCode.DUPLICATE_SERVER_DESTINATION, serverId));
    }

    private Mono<Boolean> isActiveUser(String userId) {
        return valueOperations.get(getActiveUserRedisKey(userId)).hasElement();
    }

    private String getActiveUserRedisKey(String userId) {
        return String.format("user:active:%s", userId);
    }
}
