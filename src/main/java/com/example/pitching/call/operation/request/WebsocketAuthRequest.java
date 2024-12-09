package com.example.pitching.call.operation.request;

import com.example.pitching.call.operation.Data;

public record WebsocketAuthRequest(
        String token
) implements Data {
}
