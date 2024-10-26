package com.example.pitching.call.operation.res;

import com.example.pitching.call.operation.code.ResOp;

public record HeartbeatAck(
        ResOp op,
        String seq
) implements Response {
    public static HeartbeatAck of(String seq) {
        return new HeartbeatAck(ResOp.HEARTBEAT_ACK, seq);
    }

    @Override
    public Response setValue(String seq) {
        return HeartbeatAck.of(seq);
    }
}
