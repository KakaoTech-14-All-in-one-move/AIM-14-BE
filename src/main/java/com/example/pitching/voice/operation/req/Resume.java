package com.example.pitching.voice.operation.req;

import com.example.pitching.voice.operation.Operation;
import com.example.pitching.voice.operation.code.ConnectReqOp;
import com.example.pitching.voice.operation.req.data.ResumeData;

public record Resume(
        ConnectReqOp op,
        ResumeData data
) implements Operation {
    public String getSessionId() {
        return this.data.sessionId();
    }

    public int getLastSeq() {
        return this.data.seq();
    }
}
