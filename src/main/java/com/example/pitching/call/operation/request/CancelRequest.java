package com.example.pitching.call.operation.request;

import com.example.pitching.call.operation.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CancelRequest(
        @JsonProperty("sender_id")
        String senderId
) implements Data {
}
