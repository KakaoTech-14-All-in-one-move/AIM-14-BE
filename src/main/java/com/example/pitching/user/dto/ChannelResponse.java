package com.example.pitching.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.example.pitching.user.domain.Channel;

@Schema(description = "채널 응답 정보")
public record ChannelResponse(
        @Schema(
                description = "채널 ID",
                example = "1"
        )
        Long channelId,

        @Schema(
                description = "서버 ID",
                example = "1"
        )
        Long serverId,

        @Schema(
                description = "채널 이름",
                example = "일반"
        )
        String channelName,

        @Schema(
                description = "채널 카테고리",
                example = "TEXT"
        )
        String channelCategory,

        @Schema(
                description = "채널 정렬 순서",
                example = "1"
        )
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