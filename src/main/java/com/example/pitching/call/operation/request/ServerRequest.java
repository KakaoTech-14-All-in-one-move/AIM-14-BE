package com.example.pitching.call.operation.request;

import com.example.pitching.call.operation.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ServerRequest(
        @JsonProperty("server_id")
        String serverId
) implements Data {
}
