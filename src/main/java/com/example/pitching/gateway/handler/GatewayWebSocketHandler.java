package com.example.pitching.gateway.handler;

import com.example.pitching.gateway.dto.properties.ServerProperties;
import com.example.pitching.gateway.event.HelloEvent;
import com.example.pitching.gateway.event.handler.GatewayEventHandler;
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
public class GatewayWebSocketHandler implements WebSocketHandler {

    private final ServerProperties serverProperties;
    private final GatewayEventHandler gatewayEventHandler;

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
        String jsonHelloEvent = gatewayEventHandler.eventToJson(HelloEvent.of(serverProperties.getHeartbeatInterval()));
        return session.send(Mono.just(session.textMessage(jsonHelloEvent)));
    }

    private Mono<Void> handleMessages(WebSocketSession session) {
        Flux<WebSocketMessage> responseFlux = session.receive()
                .map(message -> gatewayEventHandler.handleMessage(message, session.getId()))
                .map(session::textMessage);
        return session.send(responseFlux);
    }
}
