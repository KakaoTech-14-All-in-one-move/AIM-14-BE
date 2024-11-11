package com.example.pitching.auth.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    private String email;
    @Setter
    private String username;
    @Setter
    @Column("profile_image")
    private String profileImage;
    private String password;
    private String role;

    public static User createNewUser(String email, String username,
                                     String profileImage, String password) {
        return new User(email, username, profileImage, password, "USER");
    }
}