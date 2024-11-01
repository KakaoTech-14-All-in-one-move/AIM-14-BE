package com.example.pitching.call.operation.request;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.RequestOp;

public record Server(
        RequestOp op,
        String serverId
) implements Operation {
}
