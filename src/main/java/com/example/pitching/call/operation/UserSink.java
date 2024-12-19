package com.example.pitching.call.operation;

import com.example.pitching.call.dto.Subscription;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
@ToString
@AllArgsConstructor
public class UserSink {
    // Server Event 수신
    private final Sinks.Many<String> userSink;
    private Subscription subscription;

    public static UserSink of(Sinks.Many<String> userSink) {
        return new UserSink(userSink, null);
    }

    public void addSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public void tryEmitNext(String message) {
        this.userSink.tryEmitNext(message);
    }

    public Flux<String> getUserSinkAsFlux() {
        return this.userSink.asFlux();
    }

    public boolean doesSubscriberExists() {
        return subscription != null && !this.subscription.disposable().isDisposed();
    }

    public void dispose() {
        subscription.disposable().dispose();
    }
}
