package com.example.pitching.call.service;

import com.example.pitching.call.exception.DuplicateOperationException;
import com.example.pitching.call.exception.ErrorCode;
import com.example.pitching.call.exception.WrongAccessException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class ActiveUserManager {
    private final ReactiveValueOperations<String, String> valueOperations;

    public ActiveUserManager(ReactiveStringRedisTemplate redisTemplate) {
        this.valueOperations = redisTemplate.opsForValue();
    }

    // 서버 입장
    public Mono<String> addActiveUser(String userId, String serverId) {
        return valueOperations.getAndSet(getActiveUserRedisKey(userId), serverId)
                .filter(pastServerId -> !Objects.equals(pastServerId, serverId))
                .switchIfEmpty(Mono.error(new DuplicateOperationException(ErrorCode.DUPLICATE_SERVER_DESTINATION, serverId)));
    }

    // 로그아웃
    public Mono<Boolean> removeActiveUser(String userId) {
        return valueOperations.delete(getActiveUserRedisKey(userId));
    }

    // 채널 입장
    public Mono<String> isCorrectAccess(String userId, String serverId) {
        return valueOperations.get(getActiveUserRedisKey(userId))
                .filter(currentServerId -> Objects.equals(currentServerId, serverId))
                .switchIfEmpty(Mono.error(new WrongAccessException(ErrorCode.WRONG_ACCESS_INACTIVE_SERVER, userId)));
    }

    private String getActiveUserRedisKey(String userId) {
        return String.format("user:active:%s", userId);
    }
}
