package com.example.pitching.voice.event;

import com.example.pitching.voice.event.op.ResOp;

public record BeatEvent(ResOp op) implements GatewayEvent {
    public static BeatEvent of() {
        return new BeatEvent(ResOp.HEARTBEAT_ACK);
    }
}
