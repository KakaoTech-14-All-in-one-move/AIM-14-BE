package com.example.pitching.call.operation.response;

import com.example.pitching.call.operation.Data;

public record AnswerResponse(
        String userId,
        String sdpAnswer
) implements Data {
    public static AnswerResponse of(String userId, String sdpAnswer) {
        return new AnswerResponse(userId, sdpAnswer);
    }
}
