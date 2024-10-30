package com.example.pitching.call.operation.response;

import com.example.pitching.call.dto.ChannelType;
import com.example.pitching.call.dto.VoiceState;
import com.example.pitching.call.operation.Data;

public record ChannelEnterResponse(
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
    public static ChannelEnterResponse from(VoiceState voiceState) {
        return new ChannelEnterResponse(
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

    public static ChannelEnterResponse emtpy() {
        return new ChannelEnterResponse(null,
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
