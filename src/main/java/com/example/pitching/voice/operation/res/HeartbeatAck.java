package com.example.pitching.voice.operation.res;

import com.example.pitching.voice.operation.Operation;
import com.example.pitching.voice.operation.code.ConnectResOp;

public record HeartbeatAck(ConnectResOp op) implements Operation {
    public static HeartbeatAck of() {
        return new HeartbeatAck(ConnectResOp.HEARTBEAT_ACK);
    }
}
