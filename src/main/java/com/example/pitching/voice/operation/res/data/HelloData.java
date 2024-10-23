package com.example.pitching.voice.operation.res.data;

public record HelloData(long heartbeatInterval) {
    public static HelloData of(long interval) {
        return new HelloData(interval);
    }
}
