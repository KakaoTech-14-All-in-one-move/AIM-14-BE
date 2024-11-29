package com.example.pitching.call.service;

import com.example.pitching.call.exception.ErrorCode;
import com.example.pitching.call.exception.WrongAccessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ActiveUserManager {
    private final ReactiveValueOperations<String, String> valueOperations;
    private final Map<String, Boolean> isSubscriptionRequired = new ConcurrentHashMap<>();

    public ActiveUserManager(ReactiveStringRedisTemplate redisTemplate) {
        this.valueOperations = redisTemplate.opsForValue();
    }

    public void setSubscriptionRequired(String userId, boolean isRequired) {
        isSubscriptionRequired.put(userId, isRequired);
    }

    public boolean isSubscriptionRequired(String userId) {
        return isSubscriptionRequired.getOrDefault(userId, true);
    }

    // 서버 입장
    public Mono<Boolean> addUserActiveToRedisIfServerIdChanged(String userId, String serverId) {
        return isActiveUser(userId)
                .flatMap(isActive -> {
                    if (!isActive) return valueOperations.setIfAbsent(getActiveUserRedisKey(userId), serverId);
                    return valueOperations.getAndSet(getActiveUserRedisKey(userId), serverId)
                            .map(pastServerId -> isSameServerDestination(serverId, pastServerId))
                            .filter(Boolean.FALSE::equals)
                            .flatMap(ignored -> moveToOtherServer(userId))
                            .thenReturn(true);
                });
    }

    private Mono<Void> moveToOtherServer(String userId) {
        return Mono.fromRunnable(() -> {
            log.info("ALREADY ACTIVE BUT NOT SAME SERVER ID");
            setSubscriptionRequired(userId, true);
        });
    }

    // 로그아웃
    public Mono<String> removeUserActiveFromRedis(String userId) {
        return valueOperations.getAndDelete(getActiveUserRedisKey(userId));
    }

    // 채널 입장
    public Mono<String> isCorrectAccess(String userId, Long serverId) {
        return valueOperations.get(getActiveUserRedisKey(userId))
                .filter(currentServerId -> Objects.equals(currentServerId, String.valueOf(serverId)))
                .switchIfEmpty(Mono.error(new WrongAccessException(ErrorCode.WRONG_ACCESS_INACTIVE_SERVER, String.valueOf(serverId))));
    }

    private Boolean isSameServerDestination(String serverId, String pastServerId) {
        return Long.parseLong(serverId) == Long.parseLong(pastServerId);
    }

    private Mono<Boolean> isActiveUser(String userId) {
        return valueOperations.get(getActiveUserRedisKey(userId)).hasElement();
    }

    private String getActiveUserRedisKey(String userId) {
        return String.format("user:active:%s", userId);
    }
}
