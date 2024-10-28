package com.example.pitching.call.operation.response.data;

public record HelloData(
        long heartbeatInterval
) {
    public static HelloData of(long interval) {
        return new HelloData(interval);
    }
}
