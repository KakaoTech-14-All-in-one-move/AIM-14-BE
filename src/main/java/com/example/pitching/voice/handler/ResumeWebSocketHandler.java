package com.example.pitching.voice.handler;

import com.example.pitching.voice.operation.handler.ConnectOperationHandler;
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
public class ResumeWebSocketHandler implements WebSocketHandler {

    private final ConnectOperationHandler connectOperationHandler;

    @Override
    public List<String> getSubProtocols() {
        return WebSocketHandler.super.getSubProtocols();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return connectOperationHandler.handleMessages(session);
    }
}
