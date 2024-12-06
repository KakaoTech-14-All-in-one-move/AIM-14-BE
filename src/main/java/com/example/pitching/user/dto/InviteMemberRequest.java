package com.example.pitching.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;

@Schema(description = "서버 멤버 초대 요청")
public record InviteMemberRequest(
        @Schema(
                description = "초대할 사용자 이메일",
                example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이어야 합니다")
        String email
) {}