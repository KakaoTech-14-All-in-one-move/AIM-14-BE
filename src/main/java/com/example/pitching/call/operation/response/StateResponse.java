package com.example.pitching.call.operation.response;

import com.example.pitching.call.dto.ChannelType;
import com.example.pitching.call.dto.VoiceState;
import com.example.pitching.call.operation.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

public record StateResponse(
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("username")
        String username,
        @JsonProperty("server_id")
        Long serverId,
        @JsonProperty("channel_id")
        Long channelId,
        @JsonProperty("channel_type")
        ChannelType channelType,
        @JsonProperty("muted")
        boolean isMuted,
        @JsonProperty("deafened")
        boolean isDeafened,
        @JsonProperty("camera_on")
        boolean isCameraOn
) implements Data {
    public static StateResponse from(VoiceState voiceState) {
        return new StateResponse(
                voiceState.userId(),
                voiceState.username(),
                voiceState.serverId(),
                voiceState.channelId(),
                voiceState.channelType(),
                voiceState.isMuted(),
                voiceState.isDeafened(),
                voiceState.isCameraOn()
        );
    }
}
