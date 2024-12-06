package com.example.pitching.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 정보")
public record TokenInfo(
        @Schema(description = "액세스 토큰",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "리프레시 토큰",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken
) {}