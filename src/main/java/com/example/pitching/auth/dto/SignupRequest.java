package com.example.pitching.auth.dto;

public record SignupRequest(
        String email,
        String password,
        String username
) {}