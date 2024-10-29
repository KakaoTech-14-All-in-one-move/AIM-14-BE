package com.example.pitching.call.dto;

import com.example.pitching.call.operation.request.StateRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VoiceState {
    private String userId;
    private String username;
    private String serverId;
    private String channelId;
    private ChannelType channelType;
    private String ip;
    private Integer port;
    @JsonProperty("muted")
    private boolean isMuted;
    @JsonProperty("deafened")
    private boolean isDeafened;
    @JsonProperty("speaking")
    private boolean isSpeaking;
    @JsonProperty("cameraOn")
    private boolean isCameraOn;
    @JsonProperty("screenSharing")
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
                false,
                false,
                false,
                false,
                false);
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
