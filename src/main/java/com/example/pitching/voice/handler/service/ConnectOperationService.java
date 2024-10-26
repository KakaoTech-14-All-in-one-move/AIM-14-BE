package com.example.pitching.voice.handler.service;

import com.example.pitching.voice.dto.properties.ServerProperties;
import com.example.pitching.voice.operation.Operation;
import com.example.pitching.voice.operation.code.ConnectReqOp;
import com.example.pitching.voice.operation.req.Resume;
import com.example.pitching.voice.operation.res.*;
import com.example.pitching.voice.operation.res.data.ReadyData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectOperationService {

    private final ServerProperties serverProperties;
    private final ObjectMapper objectMapper;

    public Mono<Void> sendHello(WebSocketSession session) {
        // TODO: Redis 에 sessionId 와 seq(0) 저장
        log.info("ConnectOperationHandler.sendHello");
        String jsonHelloEvent = eventToJson(Hello.of(serverProperties.getHeartbeatInterval()));
        return session.send(Mono.just(session.textMessage(jsonHelloEvent)));
    }

    public Mono<Void> handleMessages(WebSocketSession session) {
        log.info("ConnectOperationHandler.handleMessages: sessionId = {}", session.getId());
        Flux<WebSocketMessage> responseFlux = session.receive()
                .flatMap(message -> handle(message, session.getId()))
                .doOnNext(ack -> log.info("ACK : {}", ack))
                .map(session::textMessage);
        return session.send(responseFlux);
    }

    private Flux<String> handle(WebSocketMessage webSocketMessage, String sessionId) {
        ConnectReqOp connectReqOp = readOpFromMessage(webSocketMessage);
        log.info("connectReqOp : {}", connectReqOp);
        return (switch (connectReqOp) {
            case ConnectReqOp.HEARTBEAT -> Flux.just(HeartbeatAck.of());
            case ConnectReqOp.IDENTIFY -> identify(sessionId);
            case ConnectReqOp.RESUME -> resume(webSocketMessage);
            default -> throw new RuntimeException("Unknown op code: " + connectReqOp);
        }).map(this::eventToJson);
    }

    private Flux<Ready> identify(String sessionId) {
        log.info("ConnectOperationHandler.identify");
        // TODO: Redis 에 seq(1) 로 업데이트
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> { // TODO: UserDetails 구현체로 변경
                    UserDetails user = (UserDetails) ctx.getAuthentication().getPrincipal();
                    ReadyData data = ReadyData.of(user, null, sessionId, serverProperties.getUrl());
                    return Ready.of(data);
                }).flux();
    }

    private Flux<Operation> resume(WebSocketMessage webSocketMessage) {
        Resume resume = jsonToEvent(webSocketMessage.getPayloadAsText(), Resume.class);
        String receivedSessionId = resume.getSessionId();
        int lastSeq = resume.getLastSeq();
        log.info("resume: sessionId={}, lastSeq={}", receivedSessionId, lastSeq);

        // TODO: Redis 에 저장된 sessionId 와 비교
        if (!"redisSessionId".equals(receivedSessionId)) return Flux.just(InvalidSession.of());

        // TODO: lastSeq 이후의 이벤트 가져오기
        return getEventsAfterLastSeq(receivedSessionId, lastSeq).concatWith(Mono.just(Resumed.of()));
    }

    private Flux<Operation> getEventsAfterLastSeq(String sessionId, int lastSeq) {
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

    private ConnectReqOp readOpFromMessage(WebSocketMessage webSocketMessage) {
        String jsonMessage = webSocketMessage.getPayloadAsText();
        try {
            return ConnectReqOp.from(objectMapper.readTree(jsonMessage).get("op").asInt());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
