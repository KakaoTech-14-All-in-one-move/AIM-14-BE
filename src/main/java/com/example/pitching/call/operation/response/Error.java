package com.example.pitching.call.operation.response;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ResponseOp;

import java.util.Map;

public record Error(
        ResponseOp op,
        Map<String, String> data
) implements Operation {
    public static Error of(Map<String, String> data) {
        return new Error(ResponseOp.ERROR, data);
    }

    public static Error simple(String message) {
        return Error.of(Map.of("error", message));
    }
}
