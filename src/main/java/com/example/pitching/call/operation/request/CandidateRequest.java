package com.example.pitching.call.operation.request;

import com.example.pitching.call.operation.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CandidateRequest(
        Object candidate,
        @JsonProperty("sdp_mid")
        String sdpMid,
        @JsonProperty("sdp_m_line_index")
        int sdpMLineIndex
) implements Data {
}
