package com.example.pitching.gateway.event;

import com.example.pitching.gateway.event.data.ReadyData;
import com.example.pitching.gateway.event.op.ResOp;

public record ReadyEvent(
        ResOp op,
        ReadyData data
) implements GatewayEvent {
    public static ReadyEvent of(ReadyData data) {
        return new ReadyEvent(ResOp.DISPATCH, data);
    }
}
