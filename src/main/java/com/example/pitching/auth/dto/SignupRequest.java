package com.example.pitching.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 요청 데이터")
public record SignupRequest(
        @Schema(description = "사용자 이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @Schema(description = "비밀번호", example = "password123",
                minLength = 8, maxLength = 100)
        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
        String password,

        @Schema(description = "사용자 이름", example = "홍길동",
                minLength = 2, maxLength = 50)
        @NotBlank(message = "사용자 이름은 필수입니다")
        @Size(min = 2, max = 50, message = "사용자 이름은 2-50자 사이여야 합니다")
        String username
) {}