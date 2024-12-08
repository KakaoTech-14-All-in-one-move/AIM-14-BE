package com.example.pitching.call.handler;

import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.jwt.JwtTokenProvider;
import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.call.dto.Subscription;
import com.example.pitching.call.dto.VoiceState;
import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.exception.CommonException;
import com.example.pitching.call.exception.ErrorCode;
import com.example.pitching.call.exception.InvalidValueException;
import com.example.pitching.call.exception.UnAuthorizedException;
import com.example.pitching.call.operation.*;
import com.example.pitching.call.operation.code.RequestOperation;
import com.example.pitching.call.operation.code.ResponseOperation;
import com.example.pitching.call.operation.request.*;
import com.example.pitching.call.operation.response.*;
import com.example.pitching.call.service.*;
import com.example.pitching.user.service.ChannelService;
import com.example.pitching.user.service.ServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.IceCandidate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplyHandler {
    private final Map<String, UserSink> userSinkMap = new ConcurrentHashMap<>();
    private final JwtTokenProvider jwtTokenProvider;
    private final ServerProperties serverProperties;
    private final ConvertService convertService;
    private final ServerStreamManager serverStreamManager;
    private final VoiceStateManager voiceStateManager;
    private final ActiveUserManager activeUserManager;
    private final ServerService serverService;
    private final ChannelService channelService;
    private final RoomManager roomManager;
    private final UserRegistry registry;
    private final UserRepository userRepository;

    public void cleanupResources(String userId) {
        log.debug("Clean up Resources in ReplyHandler");
        disposeSubscriptionIfPresent(userId);
        removeUserSink(userId);
        removeActiveUserFromServer(userId)
                .flatMap(serverId -> voiceStateManager.removeVoiceState(Long.valueOf(serverId), userId))
                .subscribe();
    }

    public void cleanupSession(WebSocketSession session) {
        UserSession user = registry.removeBySession(session);
        if (user == null) return;
        roomManager.getRoom(user.getChannelId()).leave(user);
    }

    public Mono<String> handleMessages(WebSocketSession session, String receivedMessage) {
        return convertService.readRequestOperationFromMessage(receivedMessage)
                .flatMap(requestOperation -> switch (requestOperation) {
                    case RequestOperation.INIT -> sendHello(session, receivedMessage);
                    case RequestOperation.HEARTBEAT -> sendHeartbeatAck();
                    case RequestOperation.SERVER -> enterServer(session, receivedMessage);
                    case RequestOperation.ENTER_CHANNEL -> enterChannel(session, receivedMessage);
                    case RequestOperation.LEAVE_CHANNEL -> leaveChannel(session, receivedMessage);
                    case RequestOperation.UPDATE_STATE -> updateState(session, receivedMessage);
                    case RequestOperation.ON_ICE_CANDIDATE -> onIceCandidate(session, receivedMessage);
                    case RequestOperation.RECEIVE_VIDEO -> receiveVideoFrom(session, receivedMessage);
                })
                .doOnNext(requestOperation -> log.debug("[{}] Send Message : {}", getUserIdFromSession(session), receivedMessage));
    }

    /**
     * @param session
     * @param receivedMessage
     * @return jsonMessage
     * 클라이언트가 처음 연결하면 토큰으로 인증과정을 거치고 Heartbeat_interval 을 담아서 응답
     */
    private Mono<String> sendHello(WebSocketSession session, String receivedMessage) {
        String token = convertService.readDataFromMessage(receivedMessage, WebsocketAuthRequest.class).token();
        Event hello = Event.of(ResponseOperation.INIT_ACK,
                HelloResponse.of(serverProperties.getHeartbeatInterval()),
                null);
        return jwtTokenProvider.validateAndGetUserId(token)
                .doOnSuccess(userId -> {
                    initializeUserSink(userId, session);
                    log.info("[{}] Connected", userId);
                })
                .then(Mono.just(convertService.convertObjectToJson(hello)));
    }

    private void initializeUserSink(String userId, WebSocketSession session) {
        session.getAttributes().put("userId", userId);
        if (userSinkMap.containsKey(userId)) return;
        userSinkMap.computeIfAbsent(userId, ignored -> {
            Sinks.Many<String> userSink = Sinks.many().unicast().onBackpressureBuffer();
            activeUserManager.setSubscriptionRequired(userId, true);
            log.debug("Create user sink : {}", userId);
            return UserSink.of(userSink);
        });
        Mono.defer(() -> sendServerEvent(userId, session)).subscribe();
    }

    private Mono<Void> sendServerEvent(String userId, WebSocketSession session) {
        return session.send(
                getMessageFromUserSink(userId)
                        .doOnNext(message -> log.debug("[{}] Server Message : {}", userId, message))
                        .onErrorResume(this::handleServerErrors)
                        .map(session::textMessage)
        );
    }

    private Flux<String> getMessageFromUserSink(String userId) {
        return userSinkMap.get(userId).getUserSinkAsFlux();
    }

    private Mono<String> handleServerErrors(Throwable e) {
        if (!(e instanceof CommonException ex)) {
            log.error("Exception occurs in handling send server messages : ", e);
            return Mono.error(e);
        }
        log.error("{} -> ", ex.getErrorCode().name(), ex);
        Event errorEvent = Event.error(ErrorResponse.from((CommonException) e));
        return Mono.just(convertService.convertObjectToJson(errorEvent));
    }

    /**
     * @return jsonMessage
     * 클라이언트가 Heartbeat 를 보내면 Heartbeat_ack 를 응답
     */
    private Mono<String> sendHeartbeatAck() {
        Event heartbeatAck = Event.of(ResponseOperation.HEARTBEAT_ACK, null, null);
        return Mono.just(convertService.convertObjectToJson(heartbeatAck));
    }

    /**
     * @param session
     * @param receivedMessage
     * @return jsonMessage
     * @apiNote 무조건 기존 서버 입장
     * 새로운 서버를 만들거나 새로운 서버에 초대된 경우 HTTP 를 사용하여 Server 참여 인원에 추가 (DB)
     */
    private Mono<String> enterServer(WebSocketSession session, String receivedMessage) {
        String userId = getUserIdFromSession(session);
        Long serverId = convertService.readDataFromMessage(receivedMessage, ServerRequest.class).serverId();
        return serverService.isValidServer(serverId)
                .filter(Boolean.TRUE::equals)
                .then(addActiveUser(userId, serverId))
                .then(userRepository.findByEmail(userId))
                .then(createServerAck(serverId))
                .doOnSuccess(ignored -> log.info("[{}] Enter server ({})", userId, serverId))
                .switchIfEmpty(Mono.error(new InvalidValueException(ErrorCode.INVALID_SERVER_ID, String.valueOf(serverId))));
    }

    private Mono<Boolean> addActiveUser(String userId, Long serverId) {
        return activeUserManager.addUserActiveToRedisIfServerIdChanged(userId, String.valueOf(serverId))
                .filter(Boolean.TRUE::equals)
                .doOnSuccess(ignored -> subscribeServerSink(serverId, userId));
    }

    private void subscribeServerSink(Long serverId, String userId) {
        // React 에서 거의 동시에 두 번 요청을 보내는 경우를 방지하기 위해 추가
        if (!activeUserManager.isSubscriptionRequired(userId)) {
            log.debug("Subscription is not required : {}", userId);
            return;
        }
        disposeSubscriptionIfPresent(userId);
        UserSink userSink = userSinkMap.get(userId);
        Disposable disposable = serverStreamManager.getMessageFromServerSink(serverId)
                .doOnNext(userSink::tryEmitNext)
                .subscribe(serverEvent -> log.debug("ServerEvent emitted to {}: {}", userId, serverEvent));
        userSink.addSubscription(Subscription.of(serverId, disposable));
        log.debug("[{}] subscribes server sink : {}", userId, serverId);
        activeUserManager.setSubscriptionRequired(userId, false);
    }

    private Mono<String> createServerAck(Long serverId) {
        return voiceStateManager.getAllVoiceState(serverId)
                .flatMap(this::createDataWithProfileImage)
                .collectList()
                .flatMap(this::createServerAckEvent)
                .switchIfEmpty(createServerAckEvent(List.of(EmptyResponse.of())));
    }

    private Mono<Data> createDataWithProfileImage(Map.Entry<String, String> mapEntry) {
        VoiceState voiceState = convertService.convertJsonToObject(mapEntry.getValue(), VoiceState.class);
        return userRepository.findByEmail(voiceState.userId())
                .map(user -> ServerResponse.from(voiceState, user.getProfileImage()));
    }

    private Mono<String> createServerAckEvent(List<Data> response) {
        Events serverAck = Events.of(ResponseOperation.SERVER_ACK, response, null);
        return Mono.just(convertService.convertObjectToJson(serverAck));
    }

    /**
     * @param session
     * @param receivedMessage
     * @return empty
     * @apiNote 음성/영상 채널 입장 (채팅 제외)
     * 직접 소켓에 응답하지 않고 server_event 로 등록
     */
    private Mono<String> enterChannel(WebSocketSession session, String receivedMessage) {
        String userId = getUserIdFromSession(session);
        ChannelRequest channelRequest = convertService.readDataFromMessage(receivedMessage, ChannelRequest.class);

        return channelService.isValidChannel(channelRequest.serverId(), channelRequest.channelId())
                .flatMap(isValid -> isValid ?
                        Mono.empty() : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, String.valueOf(channelRequest.channelId()))))
                .then(activeUserManager.isCorrectAccess(userId, channelRequest.serverId()))
                .then(userRepository.findByEmail(userId))
                .flatMap(user -> createUserAndVoiceStateTuple(userId, user, channelRequest))
                .map(tuple -> ChannelEnterResponse.from(tuple.getT1().getProfileImage(), tuple.getT2()))
                .flatMap(this::putChannelEnterToStream)
                .doOnSuccess(ignored -> {
                    joinRoom(session, channelRequest.channelId());
                    log.info("[{}] Enter {} channel ({})", userId, channelRequest.channelType(), channelRequest.channelId());
                });
    }

    private Mono<Tuple2<User, VoiceState>> createUserAndVoiceStateTuple(String userId, User user, ChannelRequest
            channelRequest) {
        return Mono.zip(
                Mono.just(user),
                Mono.just(VoiceState.from(channelRequest, user))
                        .flatMap(voiceState -> addOrChangeVoiceState(userId, channelRequest, voiceState)));
    }

    private Mono<VoiceState> addOrChangeVoiceState(String userId, ChannelRequest channelRequest, VoiceState
            voiceState) {
        return voiceStateManager.addIfAbsentOrChangeChannel(channelRequest, userId, convertService.convertObjectToJson(voiceState))
                .thenReturn(voiceState);
    }

    private Mono<String> putChannelEnterToStream(ChannelEnterResponse channelEnterResponse) {
        Event channelAck = Event.of(ResponseOperation.ENTER_CHANNEL_EVENT, channelEnterResponse, null);
        String jsonChannelAck = convertService.convertObjectToJson(channelAck);
        return serverStreamManager.addVoiceMessageToStream(channelEnterResponse.serverId(), jsonChannelAck)
                .then(Mono.empty());
    }

    private void joinRoom(WebSocketSession session, Long channelId) {
        final String userId = getUserIdFromSession(session);
        log.info("[{}]: trying to join room {}", userId, channelId);

        Room room = roomManager.getRoom(channelId);
        final UserSession user = room.join(userId, session, convertService);
        registry.register(user);
    }

    /**
     * @param session
     * @param receivedMessage
     * @return empty
     * @apiNote 음성/영상 채널 나가기 (채팅 제외)
     * 직접 소켓에 응답하지 않고 server_event 로 등록
     */
    private Mono<String> leaveChannel(WebSocketSession session, String receivedMessage) {
        String userId = getUserIdFromSession(session);
        ChannelRequest channelRequest = convertService.readDataFromMessage(receivedMessage, ChannelRequest.class);
        return channelService.isValidChannel(channelRequest.serverId(), channelRequest.channelId())
                .flatMap(isValid -> isValid ?
                        Mono.empty() : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, String.valueOf(channelRequest.channelId()))))
                .then(activeUserManager.isCorrectAccess(userId, channelRequest.serverId()))
                .then(voiceStateManager.removeVoiceState(channelRequest.serverId(), userId))
                .then(putChannelLeaveToStream(userId, channelRequest))
                .doOnSuccess(ignored -> {
                    leaveRoom(session);
                    log.info("[{}] Leave {} channel ({})", userId, channelRequest.channelType(), channelRequest.channelId());
                });
    }


    private Mono<String> putChannelLeaveToStream(String userId, ChannelRequest channelRequest) {
        Event channelAck = Event.of(ResponseOperation.LEAVE_CHANNEL_EVENT, ChannelLeaveResponse.from(channelRequest, userId), null);
        String jsonChannelAck = convertService.convertObjectToJson(channelAck);
        return serverStreamManager.addVoiceMessageToStream(channelRequest.serverId(), jsonChannelAck)
                .then(Mono.empty());
    }

    private void leaveRoom(WebSocketSession session) {
        final UserSession user = registry.getBySession(session);
        final Room room = roomManager.getRoom(user.getChannelId());
        room.leave(user);
        if (room.getParticipants().isEmpty()) {
            roomManager.removeRoom(room);
        }
    }

    /**
     * @param session
     * @param receivedMessage
     * @return empty
     * @apiNote 음성/영상 채널의 상태(옵션) 변경 (채팅 제외)
     * 직접 소켓에 응답하지 않고 server_event 로 등록
     */
    private Mono<String> updateState(WebSocketSession session, String receivedMessage) {
        String userId = getUserIdFromSession(session);
        StateRequest stateRequest = convertService.readDataFromMessage(receivedMessage, StateRequest.class);

        return channelService.isValidChannel(stateRequest.serverId(), stateRequest.channelId())
                .flatMap(isValid -> isValid ?
                        Mono.empty() : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, String.valueOf(stateRequest.channelId()))))
                .then(activeUserManager.isCorrectAccess(userId, stateRequest.serverId()))
                .then(updateStateAndGetVoiceState(userId, stateRequest))
                .flatMap(this::putUpdateStateToStream)
                .doOnSuccess(ignored -> log.info("[{}] Update state : {}", userId, stateRequest));
    }

    private Mono<VoiceState> updateStateAndGetVoiceState(String userId, StateRequest stateRequest) {
        return voiceStateManager.updateState(stateRequest, userId)
                .then(voiceStateManager.getVoiceState(stateRequest.serverId(), userId))
                .flatMap(convertService::convertJsonToVoiceState);
    }

    private Mono<String> putUpdateStateToStream(VoiceState voiceState) {
        Event stateAck = Event.of(ResponseOperation.UPDATE_STATE_EVENT, StateResponse.from(voiceState), null);
        String jsonStateAck = convertService.convertObjectToJson(stateAck);
        return serverStreamManager.addVoiceMessageToStream(voiceState.serverId(), jsonStateAck)
                .then(Mono.empty());
    }

    /**
     * @param session
     * @param receivedMessage
     * @return runnable
     */
    private Mono<String> onIceCandidate(WebSocketSession session, String receivedMessage) {
        return Mono.fromRunnable(() -> {
            final UserSession user = registry.getBySession(session);
            CandidateRequest candidateRequest = convertService.readDataFromMessage(receivedMessage, CandidateRequest.class);

            if (user != null) {
                IceCandidate candidate = new IceCandidate(candidateRequest.candidate().toString(),
                        candidateRequest.sdpMid(), candidateRequest.sdpMLineIndex());
                user.addCandidate(candidate, user.getUserId());
            }
        });
    }

    /**
     * @param session
     * @param receivedMessage
     * @return runnable
     */
    private Mono<String> receiveVideoFrom(WebSocketSession session, String receivedMessage) {
        return Mono.fromRunnable(() -> {
            OfferRequest offerRequest = convertService.readDataFromMessage(receivedMessage, OfferRequest.class);
            final UserSession user = registry.getBySession(session);
            final UserSession sender = registry.getByName(user.getUserId());
            final String sdpOffer = offerRequest.sdpOffer();
            user.receiveVideoFrom(sender, sdpOffer, convertService);
        });
    }

    private void disposeSubscriptionIfPresent(String userId) {
        UserSink userSink = userSinkMap.get(userId);
        if (userSink != null && userSink.doesSubscriberExists()) {
            userSink.dispose();
            log.debug("Dispose Subscription : {}", userId);
        }
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
