package com.example.pitching.voice.operation.res;

import com.example.pitching.voice.operation.Operation;
import com.example.pitching.voice.operation.code.ConnectResOp;

public record InvalidSession(
        ConnectResOp op
) implements Operation {
    public static InvalidSession of() {
        return new InvalidSession(ConnectResOp.INVALID_SESSION);
    }
}
