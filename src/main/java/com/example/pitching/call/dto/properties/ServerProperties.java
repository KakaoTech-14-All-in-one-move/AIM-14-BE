package com.example.pitching.call.dto.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@ConfigurationProperties("server")
public record ServerProperties(int port, Call call) {
    public String getUrl() {
        return this.call.makeUrl(this.port);
    }

    public String getUrlForTest(int port, String channel) {
        return this.call.makeUrl(port);
    }

    public long getHeartbeatInterval() {
        return this.call.heartbeatInterval.toMillis();
    }

    public Duration getTimeout() {
        return this.call.heartbeatInterval;
    }

    public String getVoicePath() {
        return this.call.websocketPath;
    }

    public record Call(String protocol, String host, String websocketPath, int version,
                       Duration heartbeatInterval) {
        private String makeUrl(int port) {
            return UriComponentsBuilder.newInstance()
                    .scheme(protocol)
                    .host(host)
                    .port(port)
                    .path(websocketPath)
                    .build()
                    .toString();
        }
    }
}
