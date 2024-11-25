package com.example.pitching.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateChannelNameRequest(
        @NotBlank(message = "채널 이름은 필수입니다.")
        String channelName
) {}
