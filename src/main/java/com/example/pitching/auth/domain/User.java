package com.example.pitching.auth.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
@Getter
public class User {
    @Id
    private final String email;
    @Setter
    private String username;
    @Setter
    @Column("profile_image")
    private String profileImage;
    private final String password;
    private final String role;

    @PersistenceCreator
    public User(String email, String username, String profileImage,
                String password, String role) {
        this.email = email;
        this.username = username;
        this.profileImage = profileImage;
        this.password = password;
        this.role = role;
    }

    public static User createNewUser(String email, String username,
                                     String profileImage, String password) {
        return new User(email, username, profileImage, password, "USER");
    }
}