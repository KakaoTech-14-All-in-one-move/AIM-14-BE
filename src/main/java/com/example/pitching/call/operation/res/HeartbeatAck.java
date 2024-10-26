package com.example.pitching.call.operation.res;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ConnectResOp;

public record HeartbeatAck(ConnectResOp op) implements Operation {
    public static HeartbeatAck of() {
        return new HeartbeatAck(ConnectResOp.HEARTBEAT_ACK);
    }
}
