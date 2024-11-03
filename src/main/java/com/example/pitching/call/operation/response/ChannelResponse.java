package com.example.pitching.call.operation.response;

import com.example.pitching.call.dto.ChannelType;
import com.example.pitching.call.dto.VoiceState;
import com.example.pitching.call.operation.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelResponse(
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("username")
        String username,
        @JsonProperty("server_id")
        String serverId,
        @JsonProperty("channel_id")
        String channelId,
        @JsonProperty("channel_type")
        ChannelType channelType,
        @JsonProperty("muted")
        boolean isMuted,
        @JsonProperty("deafened")
        boolean isDeafened,
        @JsonProperty("speaking")
        boolean isSpeaking,
        @JsonProperty("camera_on")
        boolean isCameraOn,
        @JsonProperty("screen_sharing")
        boolean isScreenSharing
) implements Data {
    public static ChannelResponse from(VoiceState voiceState) {
        return new ChannelResponse(
                voiceState.userId(),
                voiceState.username(),
                voiceState.serverId(),
                voiceState.channelId(),
                voiceState.channelType(),
                voiceState.isMuted(),
                voiceState.isDeafened(),
                voiceState.isSpeaking(),
                voiceState.isCameraOn(),
                voiceState.isScreenSharing()
        );
    }
}
