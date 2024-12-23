package com.example.pitching.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ServerMemberCounter {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "server:member:count:";

    public Mono<Long> getCurrentCount(Long serverId) {
        String key = KEY_PREFIX + serverId;
        return redisTemplate.opsForValue().get(key)
                .map(Long::parseLong)
                .defaultIfEmpty(0L);
    }

    public Mono<Long> incrementCount(Long serverId) {
        String key = KEY_PREFIX + serverId;
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count > 100) {
                        return redisTemplate.opsForValue().decrement(key)
                                .then(Mono.error(new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "서버의 최대 인원(100명)을 초과할 수 없습니다."
                                )));
                    }
                    return Mono.just(count);
                });
    }

    public Mono<Long> decrementCount(Long serverId) {
        String key = KEY_PREFIX + serverId;
        return redisTemplate.opsForValue().decrement(key)
                .flatMap(count -> {
                    if (count <= 0) {
                        // 카운트가 0 이하가 되면 키를 삭제
                        return redisTemplate.delete(key)
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                });
    }

    public Mono<Boolean> initializeCount(Long serverId, Long count) {
        String key = KEY_PREFIX + serverId;
        return redisTemplate.opsForValue().set(key, String.valueOf(count));
    }

    public Mono<Boolean> deleteCount(Long serverId) {
        String key = KEY_PREFIX + serverId;
        return redisTemplate.delete(key)
                .map(count -> count > 0);
    }
}