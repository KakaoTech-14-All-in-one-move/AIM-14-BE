package com.example.pitching.call.dto;

import com.example.pitching.call.operation.request.ChannelRequest;
import com.example.pitching.call.operation.request.StateRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

public record VoiceState(
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
) {
    public static VoiceState from(ChannelRequest channelRequest, String userId, String username) {
        return new VoiceState(
                userId,
                username,
                channelRequest.serverId(),
                channelRequest.channelId(),
                channelRequest.channelType(),
                false,
                false,
                false,
                false,
                false);
    }

    public static VoiceState from(StateRequest stateRequest) {
        return new VoiceState(
                null,
                null,
                stateRequest.serverId(),
                stateRequest.channelId(),
                null,
                stateRequest.isMuted(),
                stateRequest.isDeafened(),
                stateRequest.isSpeaking(),
                stateRequest.isCameraOn(),
                stateRequest.isScreenSharing());
    }

    public VoiceState changeChannelId(String channelId, ChannelType channelType) {
        return new VoiceState(
                this.userId,
                this.username,
                this.serverId,
                channelId,
                channelType,
                this.isMuted,
                this.isDeafened,
                this.isSpeaking,
                this.isCameraOn,
                this.isScreenSharing);
    }

    public VoiceState updateState(StateRequest stateRequest) {
        return new VoiceState(
                this.userId,
                this.username,
                this.serverId,
                this.channelId,
                this.channelType,
                stateRequest.isMuted(),
                stateRequest.isDeafened(),
                stateRequest.isSpeaking(),
                stateRequest.isCameraOn(),
                stateRequest.isScreenSharing());
    }
}
