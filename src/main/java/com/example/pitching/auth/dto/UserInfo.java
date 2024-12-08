package com.example.pitching.auth.dto;

import com.example.pitching.user.dto.ServerInfo;

import java.util.List;

public record UserInfo(
        String email,
        String username,
        Long userId,
        String profile_image,
        List<ServerInfo> servers
) {}