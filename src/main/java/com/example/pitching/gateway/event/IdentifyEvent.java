package com.example.pitching.gateway.event;

import com.example.pitching.gateway.event.data.IdentifyData;
import com.example.pitching.gateway.event.op.ReqOp;

public record IdentifyEvent(
        ReqOp reqOp,
        IdentifyData data
) implements GatewayEvent{
    public String getToken() {
        return this.data.token();
    }
}
