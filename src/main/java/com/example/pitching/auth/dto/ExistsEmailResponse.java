package com.example.pitching.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 중복 확인 응답")
public record ExistsEmailResponse(
        @Schema(description = "이메일 존재 여부", example = "true")
        boolean exists
) {}