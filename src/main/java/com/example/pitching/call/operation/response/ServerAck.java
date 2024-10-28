package com.example.pitching.call.operation.response;

import com.example.pitching.call.operation.code.ResponseOp;
import com.example.pitching.call.operation.response.data.ServerData;

public record ServerAck(
        ResponseOp op,
        ServerData data
) implements Response{
    public static ServerAck of(ServerData data) {
        return new ServerAck(ResponseOp.SERVER_ACK, data);
    }

    @Override
    public Response setSeq(String seq) {
        return null;
    }
}
