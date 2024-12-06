package com.example.pitching.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 갱신 요청 데이터")
public record RefreshRequest(
        @Schema(description = "리프레시 토큰",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @NotBlank(message = "리프레시 토큰은 필수입니다")
        String refreshToken
) {}