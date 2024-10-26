package com.example.pitching.voice.operation.req;

import com.example.pitching.voice.operation.Operation;
import com.example.pitching.voice.operation.code.ConnectReqOp;

public record Identify(
        ConnectReqOp op
) implements Operation {
}
