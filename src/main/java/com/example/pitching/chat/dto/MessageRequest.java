package com.example.pitching.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 메시지 요청 정보")
public record MessageRequest(
        @Schema(
                description = "메시지 발신자",
                example = "user@email.com"
        )
        String sender,

        @Schema(
                description = "메시지 내용",
                example = "안녕하세요!"
        )
        String content,

        @Schema(
                description = "발신자 프로필 이미지 URL",
                example = "/uploads/profile-image.jpg"
        )
        String profileImage
) {}