package com.example.pitching.call.dto;

import reactor.core.Disposable;

public record Subscription(
        String serverId,
        Disposable disposable
) {
    public static Subscription of(String serverId, Disposable disposable) {
        return new Subscription(serverId, disposable);
    }
}
