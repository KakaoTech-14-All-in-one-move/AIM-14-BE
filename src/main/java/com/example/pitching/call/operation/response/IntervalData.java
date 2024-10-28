package com.example.pitching.call.operation.response;

import com.example.pitching.call.operation.Data;

public record IntervalData(
        long heartbeatInterval
) implements Data {
    public static IntervalData of(long interval) {
        return new IntervalData(interval);
    }
}
