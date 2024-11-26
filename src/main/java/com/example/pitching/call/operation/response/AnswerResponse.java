package com.example.pitching.call.operation.response;

import com.example.pitching.call.operation.Data;

public record AnswerResponse(
        String response,
        String sdpAnswer
) implements Data {
    public static AnswerResponse ofAccepted(String sdpAnswer) {
        return new AnswerResponse("accepted", sdpAnswer);
    }

    public static AnswerResponse ofRejected(String message) {
        return new AnswerResponse("rejected", message);
    }
}
