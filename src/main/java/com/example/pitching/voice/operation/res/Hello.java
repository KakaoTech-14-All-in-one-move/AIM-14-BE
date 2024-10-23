package com.example.pitching.voice.operation.res;

import com.example.pitching.voice.operation.Operation;
import com.example.pitching.voice.operation.code.ConnectResOp;
import com.example.pitching.voice.operation.res.data.HelloData;

public record Hello(
        ConnectResOp op,
        HelloData data
) implements Operation {
    public static Hello of(long interval) {
        return new Hello(ConnectResOp.HELLO, HelloData.of(interval));
    }
}
