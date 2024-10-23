package com.example.pitching.voice.operation.req;

import com.example.pitching.voice.operation.Operation;
import com.example.pitching.voice.operation.code.ConnectReqOp;
import com.example.pitching.voice.operation.req.data.IdentifyData;

public record Identify(
        ConnectReqOp op,
        IdentifyData data
) implements Operation {
//    public String getToken() {
//        return this.data.token();
//    }
}
