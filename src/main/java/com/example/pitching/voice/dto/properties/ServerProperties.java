package com.example.pitching.voice.dto.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@ConfigurationProperties("server")
public record ServerProperties(int port, Voice voice) {
    public String getUrl() {
        return this.voice.makeUrl(this.port);
    }

    public long getHeartbeatInterval() {
        return this.voice.heartbeatInterval.toMillis();
    }

    public record Voice(String protocol, String host, String path, int version, Duration heartbeatInterval) {
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
