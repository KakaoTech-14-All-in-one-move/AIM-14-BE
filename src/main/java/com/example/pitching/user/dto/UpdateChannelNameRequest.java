package com.example.pitching.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "채널 이름 수정 요청")
public record UpdateChannelNameRequest(
        @Schema(
                description = "새로운 채널 이름",
                example = "공지사항",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "채널 이름은 필수입니다.")
        String channelName
) {}