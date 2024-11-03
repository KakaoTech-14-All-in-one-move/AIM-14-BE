package com.example.pitching.call.service;

import com.example.pitching.call.dto.UdpAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Slf4j
@Component
public class UdpAddressManager {
    private final ReactiveValueOperations<String, String> valueOperations;
    private final ConvertService convertService;

    public UdpAddressManager(ReactiveStringRedisTemplate redisTemplate, ConvertService convertService) {
        this.valueOperations = redisTemplate.opsForValue();
        this.convertService = convertService;
    }

    public Mono<Boolean> addUserAddress(String userId, InetSocketAddress sender) {
        return valueOperations.setIfAbsent(getUserAddressRedisKey(userId), convertService.convertObjectToJson(UdpAddress.of(sender)));
    }

    public Mono<UdpAddress> getUserAddress(String userId) {
        return valueOperations.get(getUserAddressRedisKey(userId))
                .map(jsonAddress -> convertService.convertJsonToObject(jsonAddress, UdpAddress.class));
    }

    public Mono<Boolean> removeUdpAddress(String userId) {
        return valueOperations.delete(getUserAddressRedisKey(userId));
    }

    public Mono<Boolean> isSameSender(String userId, InetSocketAddress sender) {
        return valueOperations.get(getUserAddressRedisKey(userId))
                .map(jsonUdpAddress -> convertService.convertJsonToObject(jsonUdpAddress, UdpAddress.class))
                .map((UdpAddress.of(sender))::equals)
                .switchIfEmpty(Mono.just(true));
    }

    private String getUserAddressRedisKey(String userId) {
        return String.format("user:address:%s", userId);
    }
}
