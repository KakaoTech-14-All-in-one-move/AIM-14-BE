package com.example.pitching.voice.operation.req.data;

public record IdentifyData(
        String token, // -> Spring Security 로 대체
        boolean compress
) {
}
