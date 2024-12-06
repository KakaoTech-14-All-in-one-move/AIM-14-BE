package com.example.pitching.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답 데이터")
public record LoginResponse(
        @Schema(description = "토큰 정보")
        TokenInfo tokenInfo,

        @Schema(description = "사용자 정보")
        UserInfo userInfo
) {}