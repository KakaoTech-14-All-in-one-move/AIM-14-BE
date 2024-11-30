package com.example.pitching.call.operation.request;

import com.example.pitching.call.operation.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

public record StateRequest(
        @JsonProperty("server_id")
        Long serverId,
        @JsonProperty("channel_id")
        Long channelId,
        @JsonProperty("muted")
        boolean isMuted,
        @JsonProperty("deafened")
        boolean isDeafened,
        @JsonProperty("camera_on")
        boolean isCameraOn
) implements Data {
}
