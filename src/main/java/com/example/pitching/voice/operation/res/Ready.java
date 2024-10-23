package com.example.pitching.voice.operation.res;

import com.example.pitching.voice.operation.Dispatch;
import com.example.pitching.voice.operation.DispatchEvent;
import com.example.pitching.voice.operation.code.ConnectResOp;
import com.example.pitching.voice.operation.res.data.ReadyData;

public record Ready(
        ConnectResOp op,
        ReadyData data,
        int seq,
        DispatchEvent event
) implements Dispatch {
    public static Ready of(ReadyData data) {
        return new Ready(DISPATCH, data, DispatchEvent.READY.getSeq(), DispatchEvent.READY);
    }
}
