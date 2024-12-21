package com.example.pitching.auth.dto;

import com.example.pitching.user.dto.ServerInfo;
import org.springframework.data.relational.core.mapping.Column;

import java.util.List;

public record UserInfo(
        String email,
        String username,
        Long user_id,
        String profile_image,
        List<ServerInfo> servers
) {}