package com.example.pitching.voice.operation.res.data;

import java.util.List;

public record ReadyData(
        Object user, // TODO: User 정보
        List<Object> servers, // TODO: 유저가 참가한 서버 리스트 -> 처음에는 unavailable 상태지만 서버가 준비되면 변경됨
        String sessionId,
        String resumeGatewayUrl
) {
    public static ReadyData of(Object user, List<Object> servers, String sessionId, String resumeGatewayUrl) {
        return new ReadyData(user, servers, sessionId, resumeGatewayUrl);
    }
}
