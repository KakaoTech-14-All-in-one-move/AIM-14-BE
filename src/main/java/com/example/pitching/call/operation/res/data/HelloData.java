package com.example.pitching.call.operation.res.data;

public record HelloData(long heartbeatInterval) {
    public static HelloData of(long interval) {
        return new HelloData(interval);
    }
}
