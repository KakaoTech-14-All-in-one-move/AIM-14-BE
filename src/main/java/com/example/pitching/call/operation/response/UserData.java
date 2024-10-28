package com.example.pitching.call.operation.response;

import com.example.pitching.call.operation.Data;

public record UserData(
        String userId,
        String username
) implements Data {
    public static UserData of(String userId, String username) {
        return new UserData(userId, username);
    }
}
