package com.example.pitching.auth.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Table("users")
@Getter
public class User {
    @Id
    private String email;
    private String username;
    @Column("profile_image")
    private String profileImage;
    private String password;
    private String role;

    @PersistenceCreator
    public User(String email, String username, String profileImage,
                String password, String role) {
        this.email = email;
        this.username = username;
        this.profileImage = profileImage;
        this.password = password;
        this.role = role;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}