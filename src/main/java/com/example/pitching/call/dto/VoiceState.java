package com.example.pitching.call.dto;

import com.example.pitching.call.operation.request.StateRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoiceState {
    private String userId;
    private String username;
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

    public static VoiceState from(StateRequest stateRequest, String userId, String username) {
        return new VoiceState(
                userId,
                username,
                stateRequest.serverId(),
                stateRequest.channelId(),
                stateRequest.channelType(),
                null,
                null,
                stateRequest.isMuted(),
                stateRequest.isDeafened(),
                stateRequest.isSpeaking(),
                stateRequest.isCameraOn(),
                stateRequest.isScreenSharing());
    }

    public VoiceState updateUdpAddress(String ip, Integer port) {
        return new VoiceState(
                this.userId,
                this.username,
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
