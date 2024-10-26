package com.example.pitching.call.operation.res;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ResOp;

public record HeartbeatAck(ResOp op) implements Operation {
    public static HeartbeatAck of() {
        return new HeartbeatAck(ResOp.HEARTBEAT_ACK);
    }
}
