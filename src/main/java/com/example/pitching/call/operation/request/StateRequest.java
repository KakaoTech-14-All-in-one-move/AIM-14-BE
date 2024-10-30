package com.example.pitching.call.operation.request;

import com.example.pitching.call.operation.Data;

public record StateRequest(
        String serverId,
        String channelId,
        boolean isMuted,
        boolean isDeafened,
        boolean isSpeaking,
        boolean isCameraOn,
        boolean isScreenSharing
) implements Data {
}
