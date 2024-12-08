package com.example.pitching.auth.domain;

import com.example.pitching.user.domain.Server;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.List;

@Table("users")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    private String email;
    @Setter
    private String username;
    @Column("user_id")
    private Long userId;
    @Setter
    @Column("profile_image")
    private String profileImage;
    private String password;
    private String role;

    @Transient  // DB에 매핑되지 않음을 표시
    private List<Server> servers = new ArrayList<>();

    public static User createNewUser(String email, String username,
                                     String profileImage, String password) {
        return new User(email, username, null, profileImage, password, "USER", new ArrayList<>());
    }

    public static User createNewUser(String email, String username,
                                     String profileImage, String password, Long userId) {
        return new User(email, username, userId, profileImage, password, "USER", new ArrayList<>());
    }
}