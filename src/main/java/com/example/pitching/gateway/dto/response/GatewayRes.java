package com.example.pitching.gateway.dto.response;

public record GatewayRes(String url) {
    public static GatewayRes of(String url) {
        return new GatewayRes(url);
    }
}
