package com.example.pitching.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CreateChannelRequest(
        @NotBlank(message = "채널 이름은 필수입니다.")
        @JsonProperty("channel_name")
        String channelName,

        @NotBlank(message = "채널 카테고리는 필수입니다.")
        @JsonProperty("channel_category")
        String channelCategory
) {}