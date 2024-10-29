package com.example.pitching.call.handler;

import com.example.pitching.call.dto.VoiceState;
import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.operation.Data;
import com.example.pitching.call.operation.Event;
import com.example.pitching.call.operation.code.RequestOperation;
import com.example.pitching.call.operation.code.ResponseOperation;
import com.example.pitching.call.operation.request.ServerRequest;
import com.example.pitching.call.operation.request.StateRequest;
import com.example.pitching.call.operation.response.EmptyResponse;
import com.example.pitching.call.operation.response.HelloResponse;
import com.example.pitching.call.operation.response.StateResponse;
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
    private final VoiceStateManager voiceStateManager;
    private final ConvertService convertService;
    private final ServerProperties serverProperties;

    private static boolean isValidServerId(String serverId) {
        return !StringUtils.hasText(serverId); // TODO: Server list에 포함되어 있는지 검사 (DB)
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
        return convertService.readRequestOperationFromMessage(receivedMessage)
                .doOnSuccess(requestOperation -> log.info("[{}] Send Message : {} -> RequestOperation : {}", userId, receivedMessage, requestOperation))
                .flatMapMany(requestOperation -> switch (requestOperation) {
                    case RequestOperation.INIT -> sendHello();
                    case RequestOperation.HEARTBEAT -> sendHeartbeatAck();
                    case RequestOperation.ENTER_SERVER -> enterServer(receivedMessage, userId);
                    case RequestOperation.ENTER_CHANNEL -> enterChannel(receivedMessage, userId);
                    case RequestOperation.LEAVE_CHANNEL -> leaveChannel(receivedMessage, userId);
                });
    }

    private Flux<String> sendHello() {
        Event hello = Event.of(ResponseOperation.HELLO,
                HelloResponse.of(serverProperties.getHeartbeatInterval()),
                null);
        return Flux.just(convertService.convertObjectToJson(hello));
    }

    private Flux<String> sendHeartbeatAck() {
        Event heartbeatAck = Event.of(ResponseOperation.HEARTBEAT_ACK, null, null);
        return Flux.just(convertService.convertObjectToJson(heartbeatAck));
    }

    // 무조건 기존 서버 입장
    // TODO: 새 서버를 만들거나 새 서버에 초대된 경우 HTTP를 사용해서 Server 참여 인원에 추가 (DB)

    private Flux<String> enterServer(String receivedMessage, String userId) {
        String serverId = convertService.readDataFromMessage(receivedMessage, ServerRequest.class).serverId();
        if (isValidServerId(serverId)) return Flux.error(new IllegalArgumentException("Invalid serverId"));
        subscribeServerSink(serverId, userId);
        // Redis에서 해당 서버의 call 중인 유저(State) 모두 조회
        return voiceStateManager.getAllVoiceState(serverId)
                .map(mapEntry -> StateResponse.from(convertService.convertJsonToData(mapEntry.getValue(), VoiceState.class)))
                .flatMap(this::createServerAck)
                .switchIfEmpty(createServerAck(EmptyResponse.of()));
    }

    private Mono<String> createServerAck(Data response) {
        Event serverAck = Event.of(ResponseOperation.SERVER_ACK, response, null);
        return Mono.just(convertService.convertObjectToJson(serverAck));
    }

    private Flux<String> enterChannel(String receivedMessage, String userId) {
        StateRequest stateRequest = convertService.readDataFromMessage(receivedMessage, StateRequest.class);
        // TODO: 1. userId로 username 조회 (DB)
        String username = "name";

        // 2. Redis(call)에 유저(State) 등록
        VoiceState voiceState = VoiceState.from(stateRequest, userId, username);
        String jsonVoiceState = convertService.convertObjectToJson(voiceState);
        return voiceStateManager.addVoiceState(userId, stateRequest.serverId(), jsonVoiceState)
                .flatMapMany(result -> {
                    if (!result) return Flux.error(new IllegalStateException("Already entered channel"));
                    Event channelAck = Event.of(ResponseOperation.ENTER_CHANNEL_ACK, StateResponse.from(voiceState), null);
                    String jsonChannelAck = convertService.convertObjectToJson(channelAck);
                    // 2. 해당 Server Stream 에 이벤트 추가
                    serverStreamManager.addVoiceMessageToStream(stateRequest.serverId(), jsonChannelAck);
                    log.info("[{}] entered the {} channel : id = {}", userId, stateRequest.channelType(), stateRequest.channelId());
                    return Flux.empty();
                });
    }

    private Flux<String> leaveChannel(String receivedMessage, String userId) {
        StateRequest stateRequest = convertService.readDataFromMessage(receivedMessage, StateRequest.class);
        return voiceStateManager.removeVoiceState(stateRequest.serverId(), userId)
                .flatMapMany(result -> {
                    if (result != 1) return Flux.error(new IllegalStateException("Already leaved channel"));
                    Event channelAck = Event.of(ResponseOperation.LEAVE_CHANNEL_ACK, null, null);
                    String jsonChannelAck = convertService.convertObjectToJson(channelAck);
                    serverStreamManager.addVoiceMessageToStream(stateRequest.serverId(), jsonChannelAck);
                    log.info("[{}] leaved the {} channel : id = {}", userId, stateRequest.channelType(), stateRequest.channelId());
                    return Flux.empty();
                });
    }

    private Mono<Void> initializeUserSink(String userId) {
        return Mono.fromRunnable(() -> {
            Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
            userSinkMap.put(userId, sink);
            log.info("Create user sink : {}", userId);
        });
    }

    private void subscribeServerSink(String serverId, String userId) {
        disposeSubscription(userId);
        Sinks.Many<String> userSink = userSinkMap.get(userId);
        Disposable disposable = serverStreamManager.getMessageFromServerSink(serverId)
                .doOnNext(userSink::tryEmitNext)
                .subscribe();
        userSubscription.put(userId, disposable);
        log.info("[{}] subscribes server sink : {}", userId, serverId);
    }

    private void disposeSubscription(String userId) {
        if (userSubscription.containsKey(userId)) {
            userSubscription.remove(userId).dispose();
            log.info("Dispose Subscription : {}", userId);
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
