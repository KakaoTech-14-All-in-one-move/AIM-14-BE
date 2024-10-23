package com.example.pitching.voice.dto.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@ConfigurationProperties("server")
public record ServerProperties(int port, Voice voice) {
    public String getUrl(boolean resume) {
        return this.voice.makeUrl(this.port, resume);
    }

    public long getHeartbeatInterval() {
        return this.voice.heartbeatInterval.toMillis();
    }

    public record Voice(String protocol, String host, String path, String resumePath, int version,
                        Duration heartbeatInterval) {
        private String makeUrl(int port, boolean resume) {
            return UriComponentsBuilder.newInstance()
                    .scheme(protocol)
                    .host(host)
                    .port(port)
                    .path(resume ? resumePath : path)
                    .queryParam("v", version)
                    .queryParam("encoding", "json")
                    .build()
                    .toString();
        }
    }
}
