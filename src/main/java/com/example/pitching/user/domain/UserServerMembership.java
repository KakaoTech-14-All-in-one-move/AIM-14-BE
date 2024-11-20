package com.example.pitching.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("user_server_memberships")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserServerMembership {
    private String email;

    @Column("server_id")
    private Long serverId;  // 필드명을 serverId로 통일

    @Column("joined_at")
    private LocalDateTime joinedAt;

    public static UserServerMembership createMembership(String email, Long serverId) {
        return new UserServerMembership(email, serverId, LocalDateTime.now());
    }
}