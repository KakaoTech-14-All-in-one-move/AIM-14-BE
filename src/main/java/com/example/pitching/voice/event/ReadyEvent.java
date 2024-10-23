package com.example.pitching.voice.event;

import com.example.pitching.voice.event.data.ReadyData;
import com.example.pitching.voice.event.op.ResOp;

public record ReadyEvent(
        ResOp op,
        ReadyData data
) implements OperationEvent {
    public static ReadyEvent of(ReadyData data) {
        return new ReadyEvent(ResOp.DISPATCH, data);
    }
}
