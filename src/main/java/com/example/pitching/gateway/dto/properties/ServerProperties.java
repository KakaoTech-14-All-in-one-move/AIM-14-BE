package com.example.pitching.gateway.dto.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@ConfigurationProperties("server")
public record ServerProperties(int port, Gateway gateway) {
    public String getUrl() {
        return this.gateway.makeUrl(this.port);
    }

    public long getHeartbeatInterval() {
        return this.gateway.heartbeatInterval.toMillis();
    }

    public record Gateway(String protocol, String host, String path, int version, Duration heartbeatInterval) {
        private String makeUrl(int port) {
            return UriComponentsBuilder.newInstance()
                    .scheme(protocol)
                    .host(host)
                    .port(port)
                    .path(path)
                    .queryParam("v", version)
                    .queryParam("encoding", "json")
                    .build()
                    .toString();
        }
    }
}
