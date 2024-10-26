package com.example.pitching.call.operation.res;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ResOp;
import com.example.pitching.call.operation.res.data.HelloData;

public record Hello(
        ResOp op,
        HelloData data
) implements Operation {
    public static Hello of(long interval) {
        return new Hello(ResOp.HELLO, HelloData.of(interval));
    }
}
