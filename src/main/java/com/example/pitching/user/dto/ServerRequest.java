package com.example.pitching.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@Schema(description = "서버 생성/수정 요청")
public record ServerRequest(
        @Schema(
                description = "서버 이름",
                example = "나의 게임 서버",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "서버 이름은 필수입니다")
        @Size(min = 1, max = 100, message = "서버 이름은 1-100자 사이여야 합니다")
        String server_name,

        @Schema(
                description = "서버 이미지 URL",
                example = "https://example.com/images/server.jpg",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @URL(message = "올바른 이미지 URL 형식이어야 합니다")
        String server_image
) {}