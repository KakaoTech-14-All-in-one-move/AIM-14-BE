package com.example.pitching.call.dto.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties("server")
public record ServerProperties(int port, Call call) {
    public String getUrl(String channel) {
        return this.call.makeUrl(this.port, channel);
    }

    public String getUrlForTest(int port, String channel) {
        return this.call.makeUrl(port, channel);
    }

    public long getHeartbeatInterval() {
        return this.call.heartbeatInterval.toMillis();
    }

    public Duration getTimeout() {
        return this.call.heartbeatInterval;
    }

    public String getVoicePath() {
        return this.call.voicePath;
    }

    public String getVideoPath() {
        return this.call.videoPath;
    }

    public record Call(String protocol, String host, String voicePath, String videoPath, int version,
                       Duration heartbeatInterval) {
        private String makeUrl(int port, String channel) {
            return UriComponentsBuilder.newInstance()
                    .scheme(protocol)
                    .host(host)
                    .port(port)
                    .path(Objects.equals(channel, "voice") ? voicePath : videoPath)
                    .build()
                    .toString();
        }
    }
}
