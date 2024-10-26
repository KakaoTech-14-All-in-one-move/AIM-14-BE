package com.example.pitching.call.operation.req;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ConnectReqOp;

public record Heartbeat(ConnectReqOp op) implements Operation {
    public static Heartbeat of() {
        return new Heartbeat(ConnectReqOp.HEARTBEAT);
    }
}
