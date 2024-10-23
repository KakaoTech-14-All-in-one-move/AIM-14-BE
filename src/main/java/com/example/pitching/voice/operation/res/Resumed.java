package com.example.pitching.voice.operation.res;

import com.example.pitching.voice.operation.Dispatch;
import com.example.pitching.voice.operation.DispatchEvent;
import com.example.pitching.voice.operation.code.ConnectResOp;

public record Resumed(
        ConnectResOp op,
        int seq,
        DispatchEvent event
) implements Dispatch {
    public static Resumed of() {
        return new Resumed(DISPATCH, DispatchEvent.RESUMED.getSeq(), DispatchEvent.RESUMED);
    }
}
