package com.example.pitching.call.operation.res;

import com.example.pitching.call.operation.code.ResOp;
import com.example.pitching.call.operation.res.data.ServerData;

public record ServerAck(
        ResOp op,
        ServerData data
) implements Response{
    public static ServerAck of(ServerData data) {
        return new ServerAck(ResOp.SERVER_ACK, data);
    }

    @Override
    public Response setSeq(String seq) {
        return null;
    }
}
