package com.example.pitching.voice.operation.res;

import com.example.pitching.voice.operation.Operation;
import com.example.pitching.voice.operation.code.ConnectResOp;

public record Beat(ConnectResOp op) implements Operation {
    public static Beat of() {
        return new Beat(ConnectResOp.HEARTBEAT_ACK);
    }
}
