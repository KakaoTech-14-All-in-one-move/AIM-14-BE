package com.example.pitching.call.operation.res;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ConnectResOp;
import com.example.pitching.call.operation.res.data.HelloData;

public record Hello(
        ConnectResOp op,
        HelloData data
) implements Operation {
    public static Hello of(long interval) {
        return new Hello(ConnectResOp.HELLO, HelloData.of(interval));
    }
}
