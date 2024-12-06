package com.example.pitching.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "채널 생성 요청")
public record CreateChannelRequest(
        @Schema(
                description = "채널 이름",
                example = "일반",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "채널 이름은 필수입니다.")
        @JsonProperty("channel_name")
        String channelName,

        @Schema(
                description = "채널 카테고리",
                example = "TEXT",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "채널 카테고리는 필수입니다.")
        @JsonProperty("channel_category")
        String channelCategory
) {}