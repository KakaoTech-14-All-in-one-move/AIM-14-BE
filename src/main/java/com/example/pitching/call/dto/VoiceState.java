package com.example.pitching.call.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoiceState {
    private String userId;
    private String serverId;
    private String channelId;
    private ChannelType channelType;
    private String ip;
    private Integer port;
    private boolean isMuted;
    private boolean isDeafened;
    private boolean isSpeaking;
    private boolean isCameraOn;
    private boolean isScreenSharing;

    public static VoiceState of(String userId, String serverId) {
        return new VoiceState(
                userId,
                serverId,
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
