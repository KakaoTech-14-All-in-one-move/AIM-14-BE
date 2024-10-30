package com.example.pitching.call.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import reactor.core.Disposable;

@Getter
@AllArgsConstructor
public class Subscription {
    private String serverId;
    private Disposable disposable;

    public static Subscription of(String serverId, Disposable disposable) {
        return new Subscription(serverId, disposable);
    }

    public static Subscription empty() {
        return new Subscription(null, null);
    }

    public void dispose() {
        this.disposable.dispose();
    }
}
