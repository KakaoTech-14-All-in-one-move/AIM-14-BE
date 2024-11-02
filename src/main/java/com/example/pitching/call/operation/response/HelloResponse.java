package com.example.pitching.call.operation.response;

import com.example.pitching.call.operation.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

public record HelloResponse(
        @JsonProperty("heartbeat_interval")
        long heartbeatInterval
) implements Data {
    public static HelloResponse of(long interval) {
        return new HelloResponse(interval);
    }
}
