package com.example.pitching.call.handler;

import com.example.pitching.call.dto.Subscription;
import com.example.pitching.call.dto.VoiceState;
import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.exception.CommonException;
import com.example.pitching.call.exception.DuplicateOperationException;
import com.example.pitching.call.exception.ErrorCode;
import com.example.pitching.call.exception.InvalidValueException;
import com.example.pitching.call.operation.Data;
import com.example.pitching.call.operation.Event;
import com.example.pitching.call.operation.code.RequestOperation;
import com.example.pitching.call.operation.code.ResponseOperation;
import com.example.pitching.call.operation.request.ChannelRequest;
import com.example.pitching.call.operation.request.ServerRequest;
import com.example.pitching.call.operation.response.*;
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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@NonNullApi
@Component
@RequiredArgsConstructor
public class CallWebSocketHandler implements WebSocketHandler {

    private final Map<String, Sinks.Many<String>> userSinkMap = new ConcurrentHashMap<>();
    private final Map<String, Subscription> userSubscription = new ConcurrentHashMap<>();
    private final ServerStreamManager serverStreamManager;
    private final VoiceStateManager voiceStateManager;
    private final ConvertService convertService;
    private final ServerProperties serverProperties;

    private static boolean isValidServerId(String serverId) {
        return StringUtils.hasText(serverId) && Objects.equals("12345", serverId); // TODO: Server list에 포함되어 있는지 검사 (DB)
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
                                                .onErrorResume(e -> handleErrors(userId, e)))
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

    private Flux<String> handleErrors(String userId, Throwable e) {
        log.error("[{}] : ", userId, e);
        if (!(e instanceof CommonException)) return Flux.error(e);
        Event errorEvent = Event.error(ErrorResponse.from((CommonException) e));
        return Flux.just(convertService.convertObjectToJson(errorEvent));
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
        if (!isValidServerId(serverId))
            return Flux.error(new InvalidValueException(ErrorCode.INVALID_SERVER_ID, serverId));
        subscribeServerSink(serverId, userId);
        // Redis에서 해당 서버의 call 중인 유저(State) 모두 조회
        return voiceStateManager.getAllVoiceState(serverId)
                .map(mapEntry -> ChannelEnterResponse.from(convertService.convertJsonToData(mapEntry.getValue(), VoiceState.class)))
                .flatMap(this::createServerAck)
                .switchIfEmpty(createServerAck(EmptyResponse.of()));
    }

    private Mono<String> createServerAck(Data response) {
        Event serverAck = Event.of(ResponseOperation.SERVER_ACK, response, null);
        return Mono.just(convertService.convertObjectToJson(serverAck));
    }

    private Flux<String> enterChannel(String receivedMessage, String userId) {
        ChannelRequest channelRequest = convertService.readDataFromMessage(receivedMessage, ChannelRequest.class);
        // TODO: userId로 username + Image Url 조회 (DB)
        String username = "name";

        VoiceState voiceState = VoiceState.from(channelRequest, userId, username);
        String jsonVoiceState = convertService.convertObjectToJson(voiceState);
        return voiceStateManager.addVoiceState(userId, channelRequest.serverId(), jsonVoiceState)
                .flatMapMany(result -> {
                    if (!result)
                        return Flux.error(new DuplicateOperationException(ErrorCode.DUPLICATED_CHANNEL_ENTRY, channelRequest.channelId()));
                    Event channelAck = Event.of(ResponseOperation.ENTER_CHANNEL_ACK, ChannelEnterResponse.from(voiceState), null);
                    String jsonChannelAck = convertService.convertObjectToJson(channelAck);
                    serverStreamManager.addVoiceMessageToStream(channelRequest.serverId(), jsonChannelAck);
                    log.info("[{}] entered the {} channel : id = {}", userId, channelRequest.channelType(), channelRequest.channelId());
                    return Flux.empty();
                });
    }

    private Flux<String> leaveChannel(String receivedMessage, String userId) {
        ChannelRequest channelRequest = convertService.readDataFromMessage(receivedMessage, ChannelRequest.class);
        return voiceStateManager.removeVoiceState(channelRequest.serverId(), userId)
                .flatMapMany(result -> {
                    if (result != 1)
                        return Flux.error(new DuplicateOperationException(ErrorCode.DUPLICATED_CHANNEL_EXIT, channelRequest.channelId()));
                    Event channelAck = Event.of(ResponseOperation.LEAVE_CHANNEL_ACK, ChannelLeaveResponse.from(channelRequest, userId), null);
                    String jsonChannelAck = convertService.convertObjectToJson(channelAck);
                    serverStreamManager.addVoiceMessageToStream(channelRequest.serverId(), jsonChannelAck);
                    log.info("[{}] leaved the {} channel : id = {}", userId, channelRequest.channelType(), channelRequest.channelId());
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
        if (isASameServer(serverId, userId))
            throw new DuplicateOperationException(ErrorCode.DUPLICATED_SERVER_DESTINATION, serverId);
        disposeSubscription(userId);
        Sinks.Many<String> userSink = userSinkMap.get(userId);
        Disposable disposable = serverStreamManager.getMessageFromServerSink(serverId)
                .doOnNext(userSink::tryEmitNext)
                .subscribe();
        userSubscription.put(userId, Subscription.of(serverId, disposable));
        log.info("[{}] subscribes server sink : {}", userId, serverId);
    }

    private boolean isASameServer(String serverId, String userId) {
        String savedServerId = userSubscription.getOrDefault(userId, Subscription.empty()).getServerId();
        return Objects.equals(savedServerId, serverId);
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
