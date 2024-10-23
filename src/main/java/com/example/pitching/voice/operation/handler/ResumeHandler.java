package com.example.pitching.voice.operation.handler;

import com.example.pitching.voice.operation.Operation;
import com.example.pitching.voice.operation.req.Resume;
import com.example.pitching.voice.operation.res.InvalidSession;
import com.example.pitching.voice.operation.res.Resumed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ResumeHandler {

    private final ObjectMapper objectMapper;

    public Mono<Void> resumeDispatchEvents(WebSocketSession session) {
        Flux<WebSocketMessage> responseFlux = session.receive()
                .flatMap(this::resume)
                .map(session::textMessage);
        return session.send(responseFlux);
    }

    private Flux<String> resume(WebSocketMessage webSocketMessage) {
        Resume resume = jsonToEvent(webSocketMessage.getPayloadAsText(), Resume.class);
        String receivedSessionId = resume.getSessionId();
        int lastSeq = resume.getLastSeq();

        // TODO: Redis 에 저장된 sessionId 와 비교
        if (!"redisSessionId".equals(receivedSessionId)) return Flux.just(eventToJson(InvalidSession.of(false)));

        // TODO: lastSeq 이후의 이벤트 가져오기
        return getEventsAfterLastSeq(receivedSessionId, lastSeq).concatWith(Mono.just(eventToJson(Resumed.of())));
    }

    private Flux<String> getEventsAfterLastSeq(String sessionId, int lastSeq) {
        return Flux.empty();
    }

    private <T extends Operation> T jsonToEvent(String jsonMessage, Class<T> eventClass) {
        try {
            return objectMapper.readValue(jsonMessage, eventClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to " + eventClass.getSimpleName(), e);
        }
    }

    private String eventToJson(Operation event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
