package com.example.pitching.gateway.event;

import com.example.pitching.gateway.event.op.ResOp;

public record BeatEvent(ResOp op) implements GatewayEvent {
    public static BeatEvent of() {
        return new BeatEvent(ResOp.HEARTBEAT_ACK);
    }
}
