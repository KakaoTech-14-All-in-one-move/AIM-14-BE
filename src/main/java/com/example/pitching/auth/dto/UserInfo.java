package com.example.pitching.auth.dto;

public record UserInfo(
        String email,
        String username,
        String profile_image
) {}