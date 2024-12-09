package com.example.pitching.chat.dto;

public record MessageRequest(
        String sender,
        String content,
        String profileImage
) {}
