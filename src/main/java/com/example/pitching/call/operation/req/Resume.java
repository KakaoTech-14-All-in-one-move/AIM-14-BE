package com.example.pitching.call.operation.req;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ReqOp;

public record Resume(
        ReqOp op,
        String seq
) implements Operation {
}
