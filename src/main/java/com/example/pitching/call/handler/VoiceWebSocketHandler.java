package com.example.pitching.call.handler;

import com.example.pitching.call.operation.code.ConnectReqOp;
import com.example.pitching.call.operation.res.HeartbeatAck;
import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@NonNullApi
@Component
@RequiredArgsConstructor
public class VoiceWebSocketHandler implements WebSocketHandler {

    private final SinkManager sinkManager;
    private final ConvertService convertService;

    @Override
    public List<String> getSubProtocols() {
        return WebSocketHandler.super.getSubProtocols();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Mono<String> userIdMono = sinkManager.getUserIdFromContext();
        return sendMessages(session, userIdMono).and(receiveMessages(session, userIdMono));
    }

    private Mono<Void> sendMessages(WebSocketSession session, Mono<String> userIdMono) {
        Flux<WebSocketMessage> messageFlux = sinkManager.registerVoice(userIdMono)
                .doOnNext(message -> log.info("RES : {}", message))
                .map(session::textMessage);
        return session.send(messageFlux);
    }

    private Flux<String> receiveMessages(WebSocketSession session, Mono<String> userIdMono) {
        return session.receive()
                .flatMap(this::handle)
                .doOnNext(message -> sinkManager.addVoiceMessage(userIdMono, message));
    }

    private Flux<String> handle(WebSocketMessage webSocketMessage) {
        ConnectReqOp connectReqOp = convertService.readOpFromMessage(webSocketMessage);
        log.info("REQ : {}", connectReqOp);
        return (switch (connectReqOp) {
            case ConnectReqOp.HEARTBEAT -> Flux.just(HeartbeatAck.of());
            default -> throw new RuntimeException("Unknown op code: " + connectReqOp);
        }).map(convertService::eventToJson);
    }
}
