package com.example.pitching.call.operation.response;

import com.example.pitching.call.dto.ChannelType;
import com.example.pitching.call.operation.Data;
import com.example.pitching.call.operation.request.ChannelRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelLeaveResponse(
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("server_id")
        String serverId,
        @JsonProperty("channel_id")
        String channelId,
        @JsonProperty("channel_type")
        ChannelType channelType
) implements Data {
    public static ChannelLeaveResponse from(ChannelRequest channelRequest, String userId) {
        return new ChannelLeaveResponse(
                userId,
                channelRequest.serverId(),
                channelRequest.channelId(),
                channelRequest.channelType()
        );
    }
}
