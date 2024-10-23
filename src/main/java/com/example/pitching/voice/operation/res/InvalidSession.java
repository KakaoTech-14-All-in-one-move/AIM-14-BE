package com.example.pitching.voice.operation.res;

import com.example.pitching.voice.operation.Operation;
import com.example.pitching.voice.operation.code.ConnectResOp;

public record InvalidSession(
        ConnectResOp op,
        boolean resume
) implements Operation {
    public static InvalidSession of(boolean d) {
        return new InvalidSession(ConnectResOp.INVALID_SESSION, d);
    }
}
