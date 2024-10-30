package com.example.pitching.call.operation.request;

import com.example.pitching.call.dto.ChannelType;
import com.example.pitching.call.operation.Data;

public record ChannelRequest(
        String serverId,
        String channelId,
        ChannelType channelType
) implements Data {
}
