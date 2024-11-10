package com.example.pitching.call.handler;

import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.jwt.JwtTokenProvider;
import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.call.dto.Subscription;
import com.example.pitching.call.dto.VoiceState;
import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.exception.*;
import com.example.pitching.call.operation.Data;
import com.example.pitching.call.operation.Event;
import com.example.pitching.call.operation.code.RequestOperation;
import com.example.pitching.call.operation.code.ResponseOperation;
import com.example.pitching.call.operation.request.ChannelRequest;
import com.example.pitching.call.operation.request.InitRequest;
import com.example.pitching.call.operation.request.ServerRequest;
import com.example.pitching.call.operation.request.StateRequest;
import com.example.pitching.call.operation.response.*;
import com.example.pitching.call.server.CallUdpClient;
import com.example.pitching.call.service.ActiveUserManager;
import com.example.pitching.call.service.ConvertService;
import com.example.pitching.call.service.ServerStreamManager;
import com.example.pitching.call.service.VoiceStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplyHandler {
    public static final String TEST_CHANNEL_ID = "5143992e-9dcd-45fe-bcc7-e337417b0cfe";
    public static final String TEST_SERVER_ID = "12345";
    private final Map<String, Sinks.Many<String>> userSinkMap = new ConcurrentHashMap<>();
    private final Map<String, Subscription> userSubscription = new ConcurrentHashMap<>();
    private final JwtTokenProvider jwtTokenProvider;
    private final ServerProperties serverProperties;
    private final ConvertService convertService;
    private final ServerStreamManager serverStreamManager;
    private final VoiceStateManager voiceStateManager;
    private final ActiveUserManager activeUserManager;
    private final CallUdpClient callUdpClient;
    private final UserRepository userRepository;
    private int i = 0;

    public Flux<String> handleMessages(String receivedMessage, WebSocketSession session) {
        return convertService.readRequestOperationFromMessage(receivedMessage)
                .flatMapMany(requestOperation -> switch (requestOperation) {
                    case RequestOperation.INIT -> sendHello(receivedMessage, session);
                    case RequestOperation.HEARTBEAT -> sendHeartbeatAck();
                    case RequestOperation.SERVER -> enterServer(receivedMessage, getUserIdFromSession(session));
                    case RequestOperation.ENTER_CHANNEL -> enterChannel(receivedMessage, getUserIdFromSession(session));
                    case RequestOperation.LEAVE_CHANNEL -> leaveChannel(receivedMessage, getUserIdFromSession(session));
                    case RequestOperation.UPDATE_STATE -> updateState(receivedMessage, getUserIdFromSession(session));
                })
                .doOnNext(requestOperation -> log.info("[{}] Send Message : {}", getUserIdFromSession(session), receivedMessage));
    }

    public void cleanupResources(String userId) {
        log.info("Clean up Resources in ReplyHandler");
        disposeSubscriptionIfPresent(userId);
        removeUserSink(userId);
        removeActiveUserFromServer(userId)
                .flatMap(serverId -> voiceStateManager.removeVoiceState(serverId, userId))
                .subscribe();
    }

    /**
     * @param receivedMessage
     * @return jsonMessage
     * 클라이언트가 처음 연결하면 토큰으로 인증과정을 거치고 Heartbeat_interval 을 담아서 응답
     */
    private Flux<String> sendHello(String receivedMessage, WebSocketSession session) {
        String token = convertService.readDataFromMessage(receivedMessage, InitRequest.class).token();
        Event hello = Event.of(ResponseOperation.INIT_ACK,
                HelloResponse.of(serverProperties.getHeartbeatInterval()),
                null);
        return jwtTokenProvider.validateAndGetUserId(token)
                .doOnSuccess(userId -> initializeUserSink(userId, session))
                .thenMany(Flux.just(convertService.convertObjectToJson(hello)));
    }

    private void initializeUserSink(String userId, WebSocketSession session) {
        session.getAttributes().put("userId", userId);
        if (userSinkMap.containsKey(userId)) return;
        userSinkMap.computeIfAbsent(userId, key -> {
            Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
            activeUserManager.setSubscriptionRequired(userId, true);
            log.info("Create user sink : {}", userId);
            return sink;
        });
        Mono.defer(() -> sendServerEvent(userId, session)).subscribe();
    }

    private Mono<Void> sendServerEvent(String userId, WebSocketSession session) {
        return session.send(
                getMessageFromUserSink(userId)
                        .doOnNext(message -> log.info("[{}] Server Message : {}", userId, message))
                        .onErrorResume(this::handleServerErrors)
                        .map(session::textMessage)
        );
    }

    private Flux<String> getMessageFromUserSink(String userId) {
        return userSinkMap.get(userId).asFlux();
    }

    private Flux<String> handleServerErrors(Throwable e) {
        if (!(e instanceof CommonException ex)) {
            log.error("Exception occurs in handling send server messages : ", e);
            return Flux.error(e);
        }
        log.error("{} -> ", ex.getErrorCode().name(), ex);
        Event errorEvent = Event.error(ErrorResponse.from((CommonException) e));
        return Flux.just(convertService.convertObjectToJson(errorEvent));
    }

    /**
     * @return jsonMessage
     * 클라이언트가 Heartbeat 를 보내면 Heartbeat_ack 를 응답
     */
    private Flux<String> sendHeartbeatAck() {
        Event heartbeatAck = Event.of(ResponseOperation.HEARTBEAT_ACK, null, null);
        return Flux.just(convertService.convertObjectToJson(heartbeatAck));
    }

    /**
     * @param receivedMessage
     * @param userId
     * @return jsonMessage
     * @apiNote 무조건 기존 서버 입장
     * 새로운 서버를 만들거나 새로운 서버에 초대된 경우 HTTP 를 사용하여 Server 참여 인원에 추가 (DB)
     * TODO: Server list에 포함되어 있는지 검사 (DB)
     */
    private Flux<String> enterServer(String receivedMessage, String userId) {
        String serverId = convertService.readDataFromMessage(receivedMessage, ServerRequest.class).serverId();
        return isValidServerId(serverId)
                .filter(Boolean.TRUE::equals)
                .then(addActiveUser(userId, serverId))
                .thenMany(createServerAck(serverId))
                .switchIfEmpty(Flux.error(new InvalidValueException(ErrorCode.INVALID_SERVER_ID, serverId)));
    }

    // TODO: DB 연결되면 로직 추가
    private Mono<Boolean> isValidServerId(String serverId) {
        return TEST_SERVER_ID.equals(serverId) ? Mono.just(true) : Mono.error(new InvalidValueException(ErrorCode.INVALID_SERVER_ID, serverId));
    }

    private Mono<Boolean> addActiveUser(String userId, String serverId) {
        return activeUserManager.addUserActiveToRedisIfServerIdChanged(userId, serverId)
                .filter(Boolean.TRUE::equals)
                .doOnSuccess(ignored -> subscribeServerSink(serverId, userId));
    }

    private void subscribeServerSink(String serverId, String userId) {
        // React 에서 거의 동시에 두 번 요청을 보내는 경우를 방지하기 위해 추가
        if (!activeUserManager.isSubscriptionRequired(userId)) {
            log.debug("Subscription is not required : {}", userId);
            return;
        }
        disposeSubscriptionIfPresent(userId);
        Sinks.Many<String> userSink = userSinkMap.get(userId);
        Disposable disposable = serverStreamManager.getMessageFromServerSink(serverId)
                .doOnNext(userSink::tryEmitNext)
                .subscribe(serverEvent -> log.info("ServerEvent emitted to {}: {}", userId, serverEvent));
        userSubscription.put(userId, Subscription.of(serverId, disposable));
        log.info("[{}] subscribes server sink : {}", userId, serverId);
        activeUserManager.setSubscriptionRequired(userId, false);
    }

    private Flux<String> createServerAck(String serverId) {
        return voiceStateManager.getAllVoiceState(serverId)
                .map(mapEntry -> StateResponse.from(convertService.convertJsonToObject(mapEntry.getValue(), VoiceState.class)))
                .flatMap(this::createServerAckEvent)
                .switchIfEmpty(createServerAckEvent(EmptyResponse.of()));
    }

    private Flux<String> createServerAckEvent(Data response) {
        Event serverAck = Event.of(ResponseOperation.SERVER_ACK, response, null);
        return Flux.just(convertService.convertObjectToJson(serverAck));
    }

    /**
     * @param receivedMessage
     * @param userId
     * @return empty
     * @apiNote 음성/영상 채널 입장 (채팅 제외)
     * 직접 소켓에 응답하지 않고 server_event 로 등록
     * TODO: Channel list에 포함되어 있는지 검사 (DB)
     */
    private Flux<String> enterChannel(String receivedMessage, String userId) {
        ChannelRequest channelRequest = convertService.readDataFromMessage(receivedMessage, ChannelRequest.class);

        return isValidChannelId(channelRequest.serverId(), channelRequest.channelId())
                .flatMap(isValid -> isValid ?
                        Mono.empty() : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, channelRequest.channelId())))
                .then(activeUserManager.isCorrectAccess(userId, channelRequest.serverId()))
                .then(callUdpClient.removeUdpAddressFromRedis(userId))
                .then(userRepository.findByEmail(userId))
                .flatMap(user -> createUserAndVoiceStateTuple(userId, user, channelRequest))
                .map(tuple -> ChannelEnterResponse.from(tuple.getT2(), tuple.getT1().getProfileImage()))
                .doOnSuccess(callUdpClient::initializeCallSink)
                .flatMapMany(this::putChannelEnterToStream);
    }

    private Mono<Tuple2<User, VoiceState>> createUserAndVoiceStateTuple(String userId, User user, ChannelRequest channelRequest) {
        return Mono.zip(
                Mono.just(user),
                Mono.just(VoiceState.from(channelRequest, user))
                        .flatMap(voiceState -> addOrChangeVoiceState(userId, channelRequest, voiceState)));
    }

    private Mono<VoiceState> addOrChangeVoiceState(String userId, ChannelRequest channelRequest, VoiceState voiceState) {
        return voiceStateManager.addIfAbsentOrChangeChannel(channelRequest, userId, convertService.convertObjectToJson(voiceState))
                .thenReturn(voiceState);
    }

    private Mono<String> putChannelEnterToStream(ChannelEnterResponse channelEnterResponse) {
        Event channelAck = Event.of(ResponseOperation.ENTER_CHANNEL_EVENT, channelEnterResponse, null);
        String jsonChannelAck = convertService.convertObjectToJson(channelAck);
        return serverStreamManager.addVoiceMessageToStream(channelEnterResponse.serverId(), jsonChannelAck)
                .doOnSuccess(ignored -> log.info("[{}] entered the {} channel : id = {}",
                        channelEnterResponse.userId(), channelEnterResponse.channelType(), channelEnterResponse.channelId()))
                .then(Mono.empty());
    }

    /**
     * @param receivedMessage
     * @param userId
     * @return empty
     * @apiNote 음성/영상 채널 나가기 (채팅 제외)
     * 직접 소켓에 응답하지 않고 server_event 로 등록
     */
    private Flux<String> leaveChannel(String receivedMessage, String userId) {
        ChannelRequest channelRequest = convertService.readDataFromMessage(receivedMessage, ChannelRequest.class);
        return isValidChannelId(channelRequest.serverId(), channelRequest.channelId())
                .flatMap(isValid -> isValid ?
                        Mono.empty() : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, channelRequest.channelId())))
                .then(activeUserManager.isCorrectAccess(userId, channelRequest.serverId()))
                .then(deleteVoiceStateIfPresent(userId, channelRequest))
                .then(callUdpClient.removeUdpAddressFromRedis(userId))
                .thenMany(putChannelLeaveToStream(userId, channelRequest));
    }

    // TODO: DB 연결되면 로직 추가
    private Mono<Boolean> isValidChannelId(String ServerId, String channelId) {
        return TEST_CHANNEL_ID.equals(channelId) ? Mono.just(true) : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, channelId));
    }

    private Mono<Long> deleteVoiceStateIfPresent(String userId, ChannelRequest channelRequest) {
        return voiceStateManager.removeVoiceState(channelRequest.serverId(), userId)
                .filter(result -> result == 1)
                .switchIfEmpty(Mono.error(new DuplicateOperationException(ErrorCode.DUPLICATE_CHANNEL_EXIT, channelRequest.channelId())));
    }

    private Mono<String> putChannelLeaveToStream(String userId, ChannelRequest channelRequest) {
        Event channelAck = Event.of(ResponseOperation.LEAVE_CHANNEL_EVENT, ChannelLeaveResponse.from(channelRequest, userId), null);
        String jsonChannelAck = convertService.convertObjectToJson(channelAck);
        return serverStreamManager.addVoiceMessageToStream(channelRequest.serverId(), jsonChannelAck)
                .doOnSuccess(ignored -> log.info("[{}] leaved the {} channel : id = {}", userId, channelRequest.channelType(), channelRequest.channelId()))
                .then(Mono.empty());
    }

    /**
     * @param receivedMessage
     * @param userId
     * @return empty
     * @apiNote 음성/영상 채널의 상태(옵션) 변경 (채팅 제외)
     * 직접 소켓에 응답하지 않고 server_event 로 등록
     */
    private Flux<String> updateState(String receivedMessage, String userId) {
        StateRequest stateRequest = convertService.readDataFromMessage(receivedMessage, StateRequest.class);

        return isValidChannelId(stateRequest.serverId(), stateRequest.channelId())
                .flatMap(isValid -> isValid ?
                        Mono.empty() : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, stateRequest.channelId())))
                .then(activeUserManager.isCorrectAccess(userId, stateRequest.serverId()))
                .then(updateStateAndGetVoiceState(userId, stateRequest))
                .flatMapMany(voiceState -> putUpdateStateToStream(userId, voiceState, stateRequest));
    }

    private Mono<VoiceState> updateStateAndGetVoiceState(String userId, StateRequest stateRequest) {
        return voiceStateManager.updateState(stateRequest, userId)
                .then(voiceStateManager.getVoiceState(stateRequest.serverId(), userId))
                .flatMap(convertService::convertJsonToVoiceState);
    }

    private Mono<String> putUpdateStateToStream(String userId, VoiceState voiceState, StateRequest stateRequest) {
        Event stateAck = Event.of(ResponseOperation.UPDATE_STATE_EVENT, StateResponse.from(voiceState), null);
        String jsonStateAck = convertService.convertObjectToJson(stateAck);
        return serverStreamManager.addVoiceMessageToStream(voiceState.serverId(), jsonStateAck)
                .doOnSuccess(ignored -> log.info("[{}] updated the state : id = {}", userId, stateRequest))
                .then(Mono.empty());
    }

    private void disposeSubscriptionIfPresent(String userId) {
        userSubscription.computeIfPresent(userId, (key, subscription) -> {
            subscription.disposable().dispose();
            log.info("Dispose Subscription : {}", userId);
            return null;
        });
    }

    private void removeUserSink(String userId) {
        userSinkMap.remove(userId);
    }

    private Mono<String> removeActiveUserFromServer(String userId) {
        return activeUserManager.removeUserActiveFromRedis(userId);
    }

    private String getUserIdFromSession(WebSocketSession session) {
        return Optional.ofNullable(session.getAttributes().get("userId"))
                .map(Object::toString)
                .orElseThrow(() -> new UnAuthorizedException(ErrorCode.UNAUTHORIZED_USER, "Anonymous"));
    }
}
