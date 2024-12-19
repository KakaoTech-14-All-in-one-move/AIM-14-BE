package com.example.pitching.call.dto;

import java.net.InetSocketAddress;

public record UdpAddress(
        String ip,
        int port
) {
    public static UdpAddress of(InetSocketAddress address) {
        return new UdpAddress(address.getAddress().getHostAddress(), address.getPort());
    }
}
