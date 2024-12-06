package com.example.pitching.user.dto;

import com.example.pitching.auth.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보 응답")
public record UserResponse(
        @Schema(description = "사용자 이메일", example = "user@example.com")
        String email,

        @Schema(description = "사용자 이름", example = "홍길동")
        String username,

        @Schema(description = "프로필 이미지 URL", example = "/uploads/550e8400-e29b-41d4-a716-446655440000_20240206123456.jpg")
        String profileImageUrl
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getEmail(),
                user.getUsername(),
                user.getProfileImage()
        );
    }
}