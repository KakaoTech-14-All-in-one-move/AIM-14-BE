package com.example.pitching.call.operation.request;

import com.example.pitching.call.operation.Data;

public record HelloData(
        long heartbeatInterval
) implements Data {
    public static HelloData of(long interval) {
        return new HelloData(interval);
    }
}
