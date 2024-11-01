package com.example.pitching.call.operation.response;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ResponseOp;
import com.example.pitching.call.operation.response.data.HelloData;

public record Hello(
        ResponseOp op,
        HelloData data
) implements Operation {
    public static Hello of(long interval) {
        return new Hello(ResponseOp.HELLO, HelloData.of(interval));
    }
}
