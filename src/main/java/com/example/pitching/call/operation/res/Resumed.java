package com.example.pitching.call.operation.res;

import com.example.pitching.call.operation.code.ResOp;

public record Resumed(
        ResOp op,
        String seq
) implements Response {
    public static Resumed of(String seq) {
        return new Resumed(ResOp.RESUMED, seq);
    }

    @Override
    public Response setSeq(String seq) {
        return Resumed.of(seq);
    }
}
