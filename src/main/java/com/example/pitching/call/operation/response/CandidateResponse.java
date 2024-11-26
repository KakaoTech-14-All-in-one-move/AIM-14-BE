package com.example.pitching.call.operation.response;

import com.example.pitching.call.operation.Data;
import org.kurento.client.IceCandidate;

public record CandidateResponse(
        IceCandidate candidate
) implements Data {
    public static CandidateResponse of(IceCandidate candidate) {
        return new CandidateResponse(candidate);
    }
}
