package com.example.pitching.call.operation.response;

import com.example.pitching.call.operation.Data;
import org.kurento.client.IceCandidate;

public record CandidateResponse(
        String userId,
        IceCandidate candidate
) implements Data {
    public static CandidateResponse of(String userId, IceCandidate candidate) {
        return new CandidateResponse(userId, candidate);
    }
}
