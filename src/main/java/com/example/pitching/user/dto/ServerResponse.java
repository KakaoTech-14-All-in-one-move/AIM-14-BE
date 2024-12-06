package com.example.pitching.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "서버 응답 정보")
public record ServerResponse(
        @Schema(description = "서버 ID",
                example = "1")
        Long serverId,

        @Schema(description = "서버 이름",
                example = "나의 게임 서버")
        String serverName,

        @Schema(
                description = "서버 이미지 URL",
                example = "/uploads/550e8400-e29b-41d4-a716-446655440000_20240206123456.jpg",
                nullable = true
        )
        String serverImage,

        @Schema(
                description = "서버 생성일시",
                example = "2024-02-06T12:34:56"
        )
        String createdAt
) {}
