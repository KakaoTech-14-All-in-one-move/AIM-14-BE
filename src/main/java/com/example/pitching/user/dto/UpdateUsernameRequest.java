package com.example.pitching.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 이름 업데이트 요청")
public record UpdateUsernameRequest(
        @Schema(description = "새로운 사용자 이름",
                example = "홍길동",
                minLength = 2,
                maxLength = 50)
        @NotBlank(message = "사용자 이름은 필수입니다")
        @Size(min = 2, max = 50, message = "사용자 이름은 2-50자 사이여야 합니다")
        String username
) {}