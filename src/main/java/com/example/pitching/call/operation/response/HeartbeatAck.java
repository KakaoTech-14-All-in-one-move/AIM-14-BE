package com.example.pitching.call.operation.response;

import com.example.pitching.call.operation.code.ResponseOp;

public record HeartbeatAck(
        ResponseOp op
) implements Response {
    public static HeartbeatAck of() {
        return new HeartbeatAck(ResponseOp.HEARTBEAT_ACK);
    }

    @Override
    public Response setSeq(String seq) {
        return null;
    }
}
