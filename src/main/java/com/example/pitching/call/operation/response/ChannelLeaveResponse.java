package com.example.pitching.call.operation.response;

import com.example.pitching.call.dto.ChannelType;
import com.example.pitching.call.operation.Data;
import com.example.pitching.call.operation.request.ChannelRequest;

public record ChannelLeaveResponse(
        String userId,
        String serverId,
        String channelId,
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
