package com.example.pitching.call.operation.request;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.RequestOp;

public record Heartbeat(RequestOp op) implements Operation {
    public static Heartbeat of() {
        return new Heartbeat(RequestOp.HEARTBEAT);
    }
}
