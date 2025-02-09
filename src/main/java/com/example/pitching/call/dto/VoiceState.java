package com.example.pitching.call.dto;

import com.example.pitching.auth.domain.User;
import com.example.pitching.call.operation.request.ChannelRequest;
import com.example.pitching.call.operation.request.StateRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

public record VoiceState(
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
        boolean isCameraOn,
        @JsonProperty("screen_sharing")
        boolean isScreenSharing
) {
    public static VoiceState from(ChannelRequest channelRequest, User user) {
        return new VoiceState(
                String.valueOf(user.getUserId()),
                user.getUsername(),
                channelRequest.serverId(),
                channelRequest.channelId(),
                channelRequest.channelType(),
                false,
                false,
                false,
                false);
    }

    public VoiceState changeChannelId(Long channelId, ChannelType channelType) {
        return new VoiceState(
                this.userId,
                this.username,
                this.serverId,
                channelId,
                channelType,
                this.isMuted,
                this.isDeafened,
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
                stateRequest.isCameraOn(),
                stateRequest.isScreenSharing());
    }
}
