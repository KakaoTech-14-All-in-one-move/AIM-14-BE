package com.example.pitching.call.operation.response;

import com.example.pitching.call.dto.ChannelType;
import com.example.pitching.call.dto.VoiceState;
import com.example.pitching.call.operation.Data;

public record ChannelResponse(
        String userId,
        String username,
        String serverId,
        String channelId,
        ChannelType channelType,
        boolean isMuted,
        boolean isDeafened,
        boolean isSpeaking,
        boolean isCameraOn,
        boolean isScreenSharing
) implements Data {
    public static ChannelResponse from(VoiceState voiceState) {
        return new ChannelResponse(
                voiceState.getUserId(),
                voiceState.getUsername(),
                voiceState.getServerId(),
                voiceState.getChannelId(),
                voiceState.getChannelType(),
                voiceState.isMuted(),
                voiceState.isDeafened(),
                voiceState.isSpeaking(),
                voiceState.isCameraOn(),
                voiceState.isScreenSharing()
        );
    }

    public static ChannelResponse emtpy() {
        return new ChannelResponse(null,
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                false,
                false);
    }
}
