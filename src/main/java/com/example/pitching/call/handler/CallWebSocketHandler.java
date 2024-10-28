package com.example.pitching.call.handler;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.operation.Event;
import com.example.pitching.call.operation.code.RequestOperation;
import com.example.pitching.call.operation.code.ResponseOperation;
import com.example.pitching.call.operation.request.HelloData;
import com.example.pitching.call.operation.request.InitData;
import com.example.pitching.call.operation.request.ServerData;
import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

    private final Map<String, Sinks.Many<String>> userSinkMap = new ConcurrentHashMap<>();
    private final Map<String, Disposable> userSubscription = new ConcurrentHashMap<>();
    private final ServerStreamManager serverStreamManager;
    private final ConvertService convertService;
    private final ServerProperties serverProperties;

    private static boolean isValidServerId(String serverId) {
        return !StringUtils.hasText(serverId); // TODO: Server list에 포함되어 있는지 검사
    }

    @Override
    public List<String> getSubProtocols() {
        return WebSocketHandler.super.getSubProtocols();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return getUserIdFromContext()
                .flatMap(userId ->
                        initializeUserSink(userId)
                                .then(replyMessages(session, Mono.just(userId))
                                        .and(sendMessages(session, Mono.just(userId))))
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
                                        .flatMap(jsonMessage -> handleMessages(jsonMessage, userId)
                                                .doOnError(error -> log.error("Error occurs in handling Reply Message()", error)))
                                        .doOnNext(message -> log.info("[{}] Reply Message : {}", userId, message))
                                        .map(session::textMessage)
                                        .doFinally(signalType -> {
                                            log.info("[{}] Disconnected: {}", userId, signalType);
                                            userSinkMap.remove(userId);
                                            disposeSubscription(userId);
                                        })
                        )
        );
    }

    private Mono<Void> sendMessages(WebSocketSession session, Mono<String> cachedUserIdMono) {
        return session.send(
                cachedUserIdMono
                        .flatMapMany(userId ->
                                getMessageFromUserSink(userId)
                                        .doOnNext(message -> log.info("[{}] Server Message : {}", userId, message))
                                        .doOnError(error -> log.error("Error occurs in handling Server Message()", error))
                                        .map(session::textMessage)
                        )
        );
    }

    private Flux<String> handleMessages(String receivedMessage, String userId) {
        RequestOperation requestOperation = convertService.readRequestOperationFromMessage(receivedMessage);
        log.info("[{}] Send Message : {}", userId, receivedMessage);
        return switch (requestOperation) {
            case RequestOperation.INIT -> sendHello(receivedMessage, userId);
            case RequestOperation.HEARTBEAT -> sendHeartbeatAck();
            case RequestOperation.SERVER -> changeServer(receivedMessage, userId);
            case RequestOperation.ENTER_CHANNEL -> enterChannel(receivedMessage, userId);
            case RequestOperation.LEAVE_CHANNEL -> Flux.empty();
        };
    }

    private Flux<String> sendHello(String receivedMessage, String userId) {
        String serverId = convertService.readDataFromMessage(receivedMessage, InitData.class).serverId();
        if (isValidServerId(serverId)) return Flux.error(new IllegalArgumentException("Invalid serverId"));
        subscribeServerSink(serverId, userId, userSinkMap.get(userId));
        // TODO: Hello 에 서버의 현재 상태 데이터 추가 (재연결 시 현재 상태 동기화를 하기 때문에 Resume 필요 X)
        Event hello = Event.of(ResponseOperation.HELLO,
                HelloData.of(serverProperties.getHeartbeatInterval()),
                null);
        return Flux.just(convertService.convertEventToJson(hello));
    }

    private Flux<String> sendHeartbeatAck() {
        Event heartbeatAck = Event.of(ResponseOperation.HEARTBEAT_ACK, null, null);
        return Flux.just(convertService.convertEventToJson(heartbeatAck));
    }

    private Flux<String> changeServer(String receivedMessage, String userId) {
        String serverId = convertService.readDataFromMessage(receivedMessage, ServerData.class).serverId();
        if (isValidServerId(serverId)) return Flux.error(new IllegalArgumentException("Invalid serverId"));
        subscribeServerSink(serverId, userId, userSinkMap.get(userId));
        // TODO: Server 에 서버의 현재 상태 데이터 추가 (재연결 시 현재 상태 동기화를 하기 때문에 Resume 필요 X)
        Event serverAck = Event.of(ResponseOperation.SERVER_ACK, null, null);
        return Flux.just(convertService.convertEventToJson(serverAck));
    }

    private Flux<String> enterChannel(String receivedMessage, String userId) {
        // State Update
        // 해당 Server Stream 에 이벤트 추가 (ServerId는 클라이언트에서 전송)
        // ENTER_SUCCESS 메세지 응답
        return Flux.empty();
    }

    private Mono<Void> initializeUserSink(String userId) {
        return Mono.fromRunnable(() -> {
            Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
            userSinkMap.put(userId, sink);
        });
    }

    private void subscribeServerSink(String serverId, String userId, Sinks.Many<String> sink) {
        disposeSubscription(userId);
        Disposable disposable = serverStreamManager.getMessageFromServerSink(serverId)
                .doOnNext(sink::tryEmitNext)
                .subscribe();
        userSubscription.put(userId, disposable);
    }

    private void disposeSubscription(String userId) {
        if (userSubscription.containsKey(userId)) {
            userSubscription.remove(userId).dispose();
        }
    }

    private Flux<String> getMessageFromUserSink(String userId) {
        return userSinkMap.get(userId).asFlux();
    }

    private Mono<String> getUserIdFromContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> (UserDetails) context.getAuthentication().getPrincipal())
                .doOnNext(userDetails -> log.info("UserDetails: {}", userDetails))
                .map(UserDetails::getUsername)
                .cache();
    }
}
