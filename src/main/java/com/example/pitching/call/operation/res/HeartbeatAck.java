package com.example.pitching.call.operation.res;

import com.example.pitching.call.operation.code.ResOp;

public record HeartbeatAck(
        ResOp op
) implements Response {
    public static HeartbeatAck of() {
        return new HeartbeatAck(ResOp.HEARTBEAT_ACK);
    }

    @Override
    public Response setSeq(String seq) {
        return null;
    }
}
