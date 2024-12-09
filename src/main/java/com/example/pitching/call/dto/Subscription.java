package com.example.pitching.call.dto;

import reactor.core.Disposable;

public record Subscription(
        Long serverId,
        Disposable disposable
) {
    public static Subscription of(Long serverId, Disposable disposable) {
        return new Subscription(serverId, disposable);
    }
}
