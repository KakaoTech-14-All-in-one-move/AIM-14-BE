package com.example.pitching.gateway.event.data;

public record IdentifyData(
        String token,
        boolean compress
) {
}
