package com.example.pitching.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ServerRequest(
        @NotBlank(message = "서버 이름은 필수입니다")
        @Size(min = 1, max = 100, message = "서버 이름은 1-100자 사이여야 합니다")
        String server_name,

        @URL(message = "올바른 이미지 URL 형식이어야 합니다")
        String server_image
) {}