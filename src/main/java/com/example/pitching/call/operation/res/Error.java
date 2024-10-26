package com.example.pitching.call.operation.res;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ResOp;

import java.util.Map;

public record Error(
        ResOp op,
        Map<String, String> data
) implements Operation {
    public static Error of(Map<String, String> data) {
        return new Error(ResOp.ERROR, data);
    }

    public static Error simple(String message) {
        return Error.of(Map.of("error", message));
    }
}
