package com.example.pitching.voice.event.handler;

import com.example.pitching.voice.dto.properties.ServerProperties;
import com.example.pitching.voice.event.BeatEvent;
import com.example.pitching.voice.event.OperationEvent;
import com.example.pitching.voice.event.ReadyEvent;
import com.example.pitching.voice.event.data.ReadyData;
import com.example.pitching.voice.event.op.ReqOp;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class OperationHandler {

    private final ServerProperties serverProperties;
    private final ObjectMapper objectMapper;

    public Mono<String> handleMessage(WebSocketMessage webSocketMessage, String sessionId) {
        ReqOp reqOp = readOpFromMessage(webSocketMessage);
        return (switch (reqOp) {
            case ReqOp.HEARTBEAT -> Mono.just(BeatEvent.of());
            case ReqOp.IDENTIFY -> identify(sessionId);
            default -> throw new RuntimeException("Unknown op code: " + reqOp);
        }).map(this::eventToJson);
    }

    public String eventToJson(OperationEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends OperationEvent> T jsonToEvent(String jsonMessage, Class<T> eventClass) {
        try {
            return objectMapper.readValue(jsonMessage, eventClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to " + eventClass.getSimpleName(), e);
        }
    }

    private Mono<OperationEvent> identify(String sessionId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> { // TODO: UserDetails 구현체로 변경
                    UserDetails user = (UserDetails) ctx.getAuthentication().getPrincipal();
                    ReadyData data = ReadyData.of(user, null, sessionId, serverProperties.getUrl());
                    return ReadyEvent.of(data);
                });
    }

    private ReqOp readOpFromMessage(WebSocketMessage webSocketMessage) {
        String jsonMessage = webSocketMessage.getPayloadAsText();
        try {
            return ReqOp.from(objectMapper.readTree(jsonMessage).get("op").asInt());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
