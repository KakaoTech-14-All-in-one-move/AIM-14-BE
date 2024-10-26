package com.example.pitching.voice.operation.req;

import com.example.pitching.voice.operation.Operation;
import com.example.pitching.voice.operation.code.ConnectReqOp;

public record Heartbeat(ConnectReqOp op) implements Operation {
    public static Heartbeat of() {
        return new Heartbeat(ConnectReqOp.HEARTBEAT);
    }
}
