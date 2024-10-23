package com.example.pitching.voice.handler;

import com.example.pitching.voice.dto.properties.ServerProperties;
import com.example.pitching.voice.event.HelloEvent;
import com.example.pitching.voice.event.handler.OperationHandler;
import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@NonNullApi
@Component
@RequiredArgsConstructor
public class VoiceWebSocketHandler implements WebSocketHandler {

    private final ServerProperties serverProperties;
    private final OperationHandler operationHandler;

    @Override
    public List<String> getSubProtocols() {
        return WebSocketHandler.super.getSubProtocols();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return sendHelloEvent(session)
                .then(handleMessages(session));
    }

    private Mono<Void> sendHelloEvent(WebSocketSession session) {
        String jsonHelloEvent = operationHandler.eventToJson(HelloEvent.of(serverProperties.getHeartbeatInterval()));
        return session.send(Mono.just(session.textMessage(jsonHelloEvent)));
    }

    private Mono<Void> handleMessages(WebSocketSession session) {
        Flux<WebSocketMessage> responseFlux = session.receive()
                .flatMap(message -> operationHandler.handleMessage(message, session.getId()))
                .map(session::textMessage);
        return session.send(responseFlux);
    }
}
