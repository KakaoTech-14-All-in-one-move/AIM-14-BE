package com.example.pitching.chat.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateMessage {
    private String userId;      // email
    private String username;
    private String profile_image;
    private String type = "USER_UPDATE";

    // 3개 파라미터를 받는 생성자 추가
    public UserUpdateMessage(String userId, String username, String profile_image) {
        this.userId = userId;
        this.username = username;
        this.profile_image = profile_image;
        this.type = "USER_UPDATE";
    }
}