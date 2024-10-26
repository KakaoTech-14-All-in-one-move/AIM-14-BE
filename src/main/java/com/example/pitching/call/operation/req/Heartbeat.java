package com.example.pitching.call.operation.req;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ReqOp;

public record Heartbeat(ReqOp op) implements Operation {
    public static Heartbeat of() {
        return new Heartbeat(ReqOp.HEARTBEAT);
    }
}
