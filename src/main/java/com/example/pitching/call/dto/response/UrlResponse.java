package com.example.pitching.call.dto.response;

public record UrlResponse(String url) {
    public static UrlResponse of(String url) {
        return new UrlResponse(url);
    }
}
