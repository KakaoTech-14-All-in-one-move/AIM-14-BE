package com.example.pitching.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUsernameRequest(
        @NotBlank(message = "사용자 이름은 필수입니다")
        @Size(min = 2, max = 50, message = "사용자 이름은 2-50자 사이여야 합니다")
        String username
) {}