package com.example.pitching.call.operation.req;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ReqOp;

public record Server(
        ReqOp op,
        String serverId
) implements Operation {
}
