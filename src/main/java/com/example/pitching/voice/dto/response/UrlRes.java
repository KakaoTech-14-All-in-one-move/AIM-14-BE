package com.example.pitching.voice.dto.response;

public record UrlRes(String url) {
    public static UrlRes of(String url) {
        return new UrlRes(url);
    }
}
