package com.example.pitching.user.dto;

import com.example.pitching.user.domain.Channel;

public record ChannelResponse(
        Long channelId,
        Long serverId,
        String channelName,
        String channelCategory,
        Integer channelPosition
) {
    public static ChannelResponse from(Channel channel) {
        return new ChannelResponse(
                channel.getChannelId(),
                channel.getServerId(),
                channel.getChannelName(),
                channel.getChannelCategory(),
                channel.getChannelPosition()
        );
    }
}