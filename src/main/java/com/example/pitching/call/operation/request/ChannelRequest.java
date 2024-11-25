package com.example.pitching.call.operation.request;

import com.example.pitching.call.dto.ChannelType;
import com.example.pitching.call.operation.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelRequest(
        @JsonProperty("server_id")
        Long serverId,
        @JsonProperty("channel_id")
        Long channelId,
        @JsonProperty("channel_type")
        ChannelType channelType
) implements Data {
}
