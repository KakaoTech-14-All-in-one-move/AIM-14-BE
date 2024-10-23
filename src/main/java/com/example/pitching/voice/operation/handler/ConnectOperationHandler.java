package com.example.pitching.voice.operation.handler;

import com.example.pitching.voice.dto.properties.ServerProperties;
import com.example.pitching.voice.operation.Operation;
import com.example.pitching.voice.operation.code.ConnectReqOp;
import com.example.pitching.voice.operation.res.Beat;
import com.example.pitching.voice.operation.res.Hello;
import com.example.pitching.voice.operation.res.Ready;
import com.example.pitching.voice.operation.res.data.ReadyData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ConnectOperationHandler {

    private final ServerProperties serverProperties;
    private final ObjectMapper objectMapper;

    public Mono<Void> sendHello(WebSocketSession session) {
        String jsonHelloEvent = eventToJson(Hello.of(serverProperties.getHeartbeatInterval()));
        return session.send(Mono.just(session.textMessage(jsonHelloEvent)));
    }

    public Mono<Void> handleMessages(WebSocketSession session) {
        Flux<WebSocketMessage> responseFlux = session.receive()
                .flatMap(message -> handle(message, session.getId()))
                .map(session::textMessage);
        return session.send(responseFlux);
    }

    private Mono<String> handle(WebSocketMessage webSocketMessage, String sessionId) {
        ConnectReqOp connectReqOp = readOpFromMessage(webSocketMessage);
        return (switch (connectReqOp) {
            case ConnectReqOp.HEARTBEAT -> Mono.just(Beat.of());
            case ConnectReqOp.IDENTIFY -> identify(sessionId);
            default -> throw new RuntimeException("Unknown op code: " + connectReqOp);
        }).map(this::eventToJson);
    }

    private String eventToJson(Operation event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Mono<Operation> identify(String sessionId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> { // TODO: UserDetails 구현체로 변경
                    UserDetails user = (UserDetails) ctx.getAuthentication().getPrincipal();
                    ReadyData data = ReadyData.of(user, null, sessionId, serverProperties.getUrl(true));
                    return Ready.of(data);
                });
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
