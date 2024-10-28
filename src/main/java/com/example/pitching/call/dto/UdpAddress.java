package com.example.pitching.call.dto;

public record UdpAddress(
        String ip,
        int port
) {
    public static UdpAddress of(String ip, int port) {
        return new UdpAddress(ip, port);
    }
}
