package com.example.pitching.call.handler;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.operation.code.ReqOp;
import com.example.pitching.call.operation.req.Resume;
import com.example.pitching.call.operation.res.HeartbeatAck;
import com.example.pitching.call.operation.res.Resumed;
import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@NonNullApi
@Component
@RequiredArgsConstructor
public class VoiceWebSocketHandler implements WebSocketHandler {

    private static final String INITIAL_SEQ = "";
    private final SinkManager sinkManager;
    private final ConvertService convertService;
    private final ServerProperties serverProperties;

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
        Flux<WebSocketMessage> webSocketMessageFlux = userIdMono
                .flatMapMany(userId -> sinkManager.registerVoice(userId)
                        .doOnNext(message -> log.info("RES for {} : {}", userId, message))
                        .map(session::textMessage));
        return session.send(webSocketMessageFlux);
    }

    private Flux<String> receiveMessages(WebSocketSession session, Mono<String> userIdMono) {
        return userIdMono
                .flatMapMany(userId -> session.receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .doOnNext(jsonMessage -> handle(jsonMessage, userId))
                        .timeout(serverProperties.getTimeout())
                        .doOnError(error -> log.error("Error occurs in receiveMessages()", error))
                        .publishOn(Schedulers.boundedElastic())
                        .doFinally(signalType -> {
                            log.info("WebSocket connection closed with signal: {}", signalType);
                            userIdMono.doOnSuccess(sinkManager::unregisterVoiceStream).subscribe();
                        }));
    }

    private void handle(String receivedMessage, String userId) {
        ReqOp reqOp = convertService.readReqOpFromMessage(receivedMessage);
        log.info("REQ : {}", reqOp);
        switch (reqOp) {
            case ReqOp.HEARTBEAT -> sendHeartbeatAck(userId);
            case ReqOp.RESUME -> resumeMissedEvent(receivedMessage, userId);
        }
        ;
    }

    private void sendHeartbeatAck(String userId) {
        String heartbeatAck = convertService.eventToJson(HeartbeatAck.of(INITIAL_SEQ));
        sinkManager.addVoiceMessageToStream(userId, heartbeatAck);
    }

    private void resumeMissedEvent(String jsonMessage, String userId) {
        Resume resume = convertService.jsonToEvent(jsonMessage, Resume.class);
        sinkManager.addMissedVoiceMessageToStream(userId, resume.seq());
        String resumed = convertService.eventToJson(Resumed.of(INITIAL_SEQ));
        sinkManager.addVoiceMessageToStream(userId, resumed);
    }
}
