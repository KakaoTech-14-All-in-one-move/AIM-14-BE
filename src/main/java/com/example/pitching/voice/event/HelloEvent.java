package com.example.pitching.voice.event;

import com.example.pitching.voice.event.data.HelloData;
import com.example.pitching.voice.event.op.ResOp;

public record HelloEvent(
        ResOp op,
        HelloData data
) implements OperationEvent {
    public static HelloEvent of(long interval) {
        return new HelloEvent(ResOp.HELLO, HelloData.of(interval));
    }
}
