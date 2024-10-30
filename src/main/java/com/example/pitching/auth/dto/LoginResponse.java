package com.example.pitching.auth.dto;

public record LoginResponse(
        TokenInfo tokenInfo,
        UserInfo userInfo
) {}