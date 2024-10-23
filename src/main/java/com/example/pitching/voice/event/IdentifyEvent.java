package com.example.pitching.voice.event;

import com.example.pitching.voice.event.data.IdentifyData;
import com.example.pitching.voice.event.op.ReqOp;

public record IdentifyEvent(
        ReqOp reqOp,
        IdentifyData data
) implements GatewayEvent{
    public String getToken() {
        return this.data.token();
    }
}
