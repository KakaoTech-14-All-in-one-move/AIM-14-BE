package com.example.pitching.user.dto;

public record ServerResponse(
        Long server_id,
        String server_name,
        String server_image,
        String created_at
) {}