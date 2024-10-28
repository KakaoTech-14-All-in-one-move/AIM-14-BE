package com.example.pitching.call.operation.response;

import com.example.pitching.call.dto.ChannelType;
import com.example.pitching.call.dto.VoiceState;
import com.example.pitching.call.operation.Data;

public record VoiceStateData(
        String userId,
        String username,
        String serverId,
        String channelId,
        ChannelType channelType,
        String ip,
        Integer port,
        boolean isMuted,
        boolean isDeafened,
        boolean isSpeaking,
        boolean isCameraOn,
        boolean isScreenSharing
) implements Data {
    public static VoiceStateData of(VoiceState voiceState, String username) {
        return new VoiceStateData(
                voiceState.getUserId(),
                username,
                voiceState.getServerId(),
                voiceState.getChannelId(),
                voiceState.getChannelType(),
                voiceState.getIp(),
                voiceState.getPort(),
                voiceState.isMuted(),
                voiceState.isDeafened(),
                voiceState.isSpeaking(),
                voiceState.isCameraOn(),
                voiceState.isScreenSharing()
        );
    }
}
