package com.example.pitching.call.handler.service;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ConnectReqOp;
import com.example.pitching.call.operation.res.HeartbeatAck;
import com.example.pitching.call.operation.res.Hello;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            default -> throw new RuntimeException("Unknown op code: " + connectReqOp);
        }).map(this::eventToJson);
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
