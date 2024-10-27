package com.example.pitching.call.handler;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.operation.code.ReqOp;
import com.example.pitching.call.operation.req.Init;
import com.example.pitching.call.operation.res.HeartbeatAck;
import com.example.pitching.call.operation.res.Hello;
import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@NonNullApi
@Component
@RequiredArgsConstructor
public class CallWebSocketHandler implements WebSocketHandler {

    public static final Map<String, Sinks.Many<String>> userSinkMap = new ConcurrentHashMap<>();
    public static final Map<String, Disposable> userSubscription = new ConcurrentHashMap<>();
    private static final String INITIAL_SEQ = null;
    private final ServerStreamManager serverStreamManager;
    private final ConvertService convertService;
    private final ServerProperties serverProperties;

    @Override
    public List<String> getSubProtocols() {
        return WebSocketHandler.super.getSubProtocols();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return serverStreamManager.getUserIdFromContext()
                .flatMap(userId ->
                        initializeUserSink(userId)
                                .then(Mono.when(
                                        replyMessages(session, Mono.just(userId)),
                                        sendMessages(session, Mono.just(userId))
                                ))
                );
    }

    private Mono<Void> replyMessages(WebSocketSession session, Mono<String> cachedUserIdMono) {
        return session.send(
                cachedUserIdMono
                        .doOnSuccess(userId -> log.info("[{}] Connected", userId))
                        .flatMapMany(userId ->
                                session.receive()
                                        .timeout(serverProperties.getTimeout())
                                        .map(WebSocketMessage::getPayloadAsText)
                                        .flatMap(jsonMessage -> handle(jsonMessage, userId)
                                                .doOnError(error -> log.error("Error occurs in handling Reply Message()", error)))
                                        .doOnNext(message -> log.info("[{}] Reply Message : {}", userId, message))
                                        .map(session::textMessage)
                                        .doFinally(signalType -> {
                                            log.info("WebSocket connection closed with signal: {}", signalType);
                                            userSinkMap.remove(userId);
                                        })
                        )
        );
    }

    private Mono<Void> sendMessages(WebSocketSession session, Mono<String> cachedUserIdMono) {
        return session.send(
                cachedUserIdMono
                        .flatMapMany(userId ->
                                getMessageFromUserSink(userId)
                                        .onBackpressureBuffer()
                                        .doOnNext(message -> log.info("[{}] Server Message : {}", userId, message))
                                        .doOnError(error -> log.error("Error occurs in handling Server Message()", error))
                                        .map(session::textMessage)
                        )
        );
    }

    private Flux<String> handle(String receivedMessage, String userId) {
        ReqOp reqOp = convertService.readReqOpFromMessage(receivedMessage);
        log.info("[{}] Send Message : {}", userId, receivedMessage);
        return switch (reqOp) {
            case ReqOp.INIT -> sendHello(receivedMessage, userId);
            case ReqOp.HEARTBEAT -> sendHeartbeatAck();
//            default -> Flux.empty();
        };
    }

    private Flux<String> sendHello(String receivedMessage, String userId) {
        String serverId = convertService.jsonToEvent(receivedMessage, Init.class).serverId();
        if (!StringUtils.hasText(serverId)) return Flux.error(new IllegalArgumentException("Invalid serverId"));
        subscribeServerSink(serverId, userId, userSinkMap.get(userId));
        // Hello 에 서버의 현재 상태 데이터 추가 (재연결 시 현재 상태 동기화를 하기 때문에 Resume 필요 X)
        String helloMessage = convertService.eventToJson(Hello.of(serverProperties.getHeartbeatInterval()));
        return Flux.just(helloMessage);
    }

    private Flux<String> sendHeartbeatAck() {
        String ackMessage = convertService.eventToJson(HeartbeatAck.of());
        return Flux.just(ackMessage);
    }

    private Mono<Void> initializeUserSink(String userId) {
        return Mono.fromRunnable(() -> {
            Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
            userSinkMap.put(userId, sink);
        });
    }

    private void subscribeServerSink(String serverId, String userId, Sinks.Many<String> sink) {
        if (userSubscription.containsKey(userId)) {
            userSubscription.remove(userId).dispose();
        }
        Disposable disposable = serverStreamManager.getMessageFromServerSink(serverId)
                .doOnNext(sink::tryEmitNext)
                .subscribe();
        userSubscription.put(userId, disposable);
    }

    private Flux<String> getMessageFromUserSink(String userId) {
        return userSinkMap.get(userId).asFlux();
    }
}
