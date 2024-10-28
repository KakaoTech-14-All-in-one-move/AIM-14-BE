package com.example.pitching.call.dto;

import com.example.pitching.call.operation.response.VoiceStateData;
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

    public static VoiceState from(VoiceStateData voiceStateData) {
        return new VoiceState(
                voiceStateData.userId(),
                voiceStateData.serverId(),
                voiceStateData.channelId(),
                voiceStateData.channelType(),
                voiceStateData.ip(),
                voiceStateData.port(),
                voiceStateData.isMuted(),
                voiceStateData.isDeafened(),
                voiceStateData.isSpeaking(),
                voiceStateData.isCameraOn(),
                voiceStateData.isScreenSharing());
    }

    public VoiceState updateUdpAddress(String ip, Integer port) {
        return new VoiceState(
                this.userId,
                this.serverId,
                this.channelId,
                this.channelType,
                ip,
                port,
                this.isMuted,
                this.isDeafened,
                this.isSpeaking,
                this.isCameraOn,
                this.isScreenSharing);
    }
}
