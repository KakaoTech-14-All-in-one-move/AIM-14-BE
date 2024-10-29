package com.example.pitching.call.operation.response;

import com.example.pitching.call.operation.Data;

public record HelloResponse(
        long heartbeatInterval
) implements Data {
    public static HelloResponse of(long interval) {
        return new HelloResponse(interval);
    }
}
