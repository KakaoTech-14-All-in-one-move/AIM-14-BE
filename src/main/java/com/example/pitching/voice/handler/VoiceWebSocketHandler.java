package com.example.pitching.voice.handler;

import com.example.pitching.voice.handler.service.ConnectOperationService;
import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.List;

@NonNullApi
@Component
@RequiredArgsConstructor
public class VoiceWebSocketHandler implements WebSocketHandler {

    private final ConnectOperationService connectOperationService;

    @Override
    public List<String> getSubProtocols() {
        return WebSocketHandler.super.getSubProtocols();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return connectOperationService.sendHello(session)
                .and(connectOperationService.handleMessages(session));
    }
}
