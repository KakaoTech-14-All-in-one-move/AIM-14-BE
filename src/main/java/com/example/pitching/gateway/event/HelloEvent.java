package com.example.pitching.gateway.event;

import com.example.pitching.gateway.event.data.HelloData;
import com.example.pitching.gateway.event.op.ResOp;

public record HelloEvent(
        ResOp op,
        HelloData data
) implements GatewayEvent {
    public static HelloEvent of(long interval) {
        return new HelloEvent(ResOp.HELLO, HelloData.of(interval));
    }
}
