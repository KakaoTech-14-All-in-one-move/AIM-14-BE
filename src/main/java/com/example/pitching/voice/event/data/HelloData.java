package com.example.pitching.voice.event.data;

public record HelloData(long heartbeatInterval) {
    public static HelloData of(long interval) {
        return new HelloData(interval);
    }
}
