package com.example.pitching.call.operation.response;

import com.example.pitching.call.dto.ChannelType;
import com.example.pitching.call.dto.VoiceState;
import com.example.pitching.call.operation.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ServerResponse(
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("username")
        String username,
        @JsonProperty("profile_image")
        String profileImage,
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
    public static ServerResponse from(VoiceState voiceState, String profileImage) {
        return new ServerResponse(
                voiceState.userId(),
                voiceState.username(),
                profileImage,
                voiceState.serverId(),
                voiceState.channelId(),
                voiceState.channelType(),
                voiceState.isMuted(),
                voiceState.isDeafened(),
                voiceState.isCameraOn()
        );
    }
}
