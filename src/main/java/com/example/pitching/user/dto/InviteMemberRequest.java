package com.example.pitching.user.dto;

import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @NotNull(message = "이메일은 필수입니다")
        String email
) {}