package com.example.pitching.user.dto;

import com.example.pitching.auth.domain.User;

public record UserResponse(
        String email,
        String username,
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