package com.example.pitching.call.operation.request;

import com.example.pitching.call.operation.Data;

public record CandidateRequest(
        String candidate,
        String sdpMid,
        int sdpMLineIndex
) implements Data {
}
