package com.example.pitching.call.handler;

import com.example.pitching.call.exception.DuplicateOperationException;
import com.example.pitching.call.exception.ErrorCode;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ActiveUserManager {
    private final ReactiveSetOperations<String, String> setOperations;

    public ActiveUserManager(ReactiveStringRedisTemplate redisTemplate) {
        this.setOperations = redisTemplate.opsForSet();
    }

    public Mono<Long> enterServer(String serverId, String userId) {
        return isActiveUser(serverId, userId)
                .flatMap(result -> {
                    if (result)
                        return Mono.error(new DuplicateOperationException(ErrorCode.DUPLICATE_SERVER_DESTINATION, serverId));
                    return inactivateUser(serverId, userId)
                            .flatMap(result2 -> activateUser(serverId, userId));
                });

    }

    private Mono<Long> activateUser(String serverId, String userId) {
        return setOperations.add(getActiveUserRedisKey(serverId), userId);
    }

    private Mono<Long> inactivateUser(String serverId, String userId) {
        return setOperations.remove(getActiveUserRedisKey(serverId), userId);
    }

    public Mono<Boolean> isActiveUser(String serverId, String userId) {
        return setOperations.isMember(getActiveUserRedisKey(serverId), userId);
    }

    private String getActiveUserRedisKey(String serverId) {
        return String.format("server:%s:users", serverId);
    }
}
