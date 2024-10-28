package com.example.pitching.call.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@AllArgsConstructor
public class VoiceState {
    @Getter
    private String userId;
    @Getter
    private String serverId;
    private String channelId;
    private ChannelType channelType;
    private boolean isMuted;
    private boolean isDeafened;
    private boolean isSpeaking;
    private boolean isCameraOn;
    private boolean isScreenSharing;
    private Instant joinTime;

    public static VoiceState of(String userId, String serverId) {
        return new VoiceState(
                userId,
                serverId,
                null,
                null,
                false,
                false,
                false,
                false,
                false,
                null);
    }
}
