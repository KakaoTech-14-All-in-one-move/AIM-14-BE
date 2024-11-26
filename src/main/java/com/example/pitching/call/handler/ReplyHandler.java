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
import com.example.pitching.call.operation.UserSession;
import com.example.pitching.call.operation.code.RequestOperation;
import com.example.pitching.call.operation.code.ResponseOperation;
import com.example.pitching.call.operation.request.*;
import com.example.pitching.call.operation.response.*;
import com.example.pitching.call.service.ActiveUserManager;
import com.example.pitching.call.service.ConvertService;
import com.example.pitching.call.service.ServerStreamManager;
import com.example.pitching.call.service.VoiceStateManager;
import com.example.pitching.user.service.ServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.KurentoClient;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplyHandler {
    public static final Long TEST_VOICE_CHANNEL_ID = 1L;
    public static final Long TEST_VIDEO_CHANNEL_ID = 2L;
    private final Map<String, UserSession> userSessionMap = new ConcurrentHashMap<>();
    private final Map<Long, UserSession> presenterMap = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> viewerMap = new ConcurrentHashMap<>();
    private final JwtTokenProvider jwtTokenProvider;
    private final ServerProperties serverProperties;
    private final ConvertService convertService;
    private final ServerStreamManager serverStreamManager;
    private final VoiceStateManager voiceStateManager;
    private final ActiveUserManager activeUserManager;
    private final ServerService serverService;
    private final KurentoClient kurento;
    private final UserRepository userRepository;

    public Flux<String> handleMessages(final WebSocketSession session, String receivedMessage) {
        return convertService.readRequestOperationFromMessage(receivedMessage)
                .flatMapMany(requestOperation -> switch (requestOperation) {
                    case RequestOperation.INIT -> sendHello(session, receivedMessage);
                    case RequestOperation.HEARTBEAT -> sendHeartbeatAck();
                    case RequestOperation.SERVER -> enterServer(session, receivedMessage);
                    case RequestOperation.ENTER_CHANNEL -> enterChannel(session, receivedMessage);
                    case RequestOperation.LEAVE_CHANNEL -> leaveChannel(session, receivedMessage);
                    case RequestOperation.UPDATE_STATE -> updateState(session, receivedMessage);
                    case RequestOperation.PRESENTER -> presenter(session, receivedMessage);
                    case RequestOperation.VIEWER -> viewer(session, receivedMessage);
                    case RequestOperation.ON_ICE_CANDIDATE -> onIceCandidate(session, receivedMessage);
                    case RequestOperation.STOP -> stop(session);
                })
                .doOnNext(requestOperation -> log.info("[{}] Send Message : {}", getUserIdFromSession(session), receivedMessage));
    }

    private Mono<String> stop(WebSocketSession session) {
        Long channelId = 1L;
        String userId = getUserIdFromSession(session);
        if (presenterMap.get(channelId) != null && presenterMap.get(channelId).isSameSession(session)) {
            for (String viewer : viewerMap.get(channelId)) {
                String message = convertService.convertObjectToJson(
                        Event.of(ResponseOperation.STOP_COMMUNICATION, null, null));
                userSessionMap.get(viewer).sendMessage(message);
            }
            log.info("Releasing media pipeline");
            presenterMap.get(channelId).releaseMediaPipeline();
            presenterMap.remove(channelId);
        } else if (viewerMap.get(channelId).contains(userId)) {
            userSessionMap.get(userId).releaseWebRtcEndpoint();
            viewerMap.get(channelId).remove(userId);
        }
        return Mono.empty();
    }

    private Mono<String> onIceCandidate(WebSocketSession session, String receivedMessage) {
        Long channelId = 1L;
        String userId = getUserIdFromSession(session);
        UserSession user = null;
        if (!presenterMap.containsKey(channelId)) {
            if (presenterMap.get(channelId).isSameSession(session)) {
                user = presenterMap.get(channelId);
            } else {
                user = userSessionMap.get(userId);
            }
        }
        if (user != null) {
            CandidateRequest candidateRequest = convertService.readDataFromMessage(receivedMessage, CandidateRequest.class);
            IceCandidate iceCandidate = new IceCandidate(candidateRequest.candidate(), candidateRequest.sdpMid(), candidateRequest.sdpMLineIndex());
            user.addCandidate(iceCandidate);
        }
        return Mono.empty();
    }

    private Mono<String> viewer(WebSocketSession session, String receivedMessage) {
        Long channelId = 1L;
        String userId = getUserIdFromSession(session);
        if (!presenterMap.containsKey(channelId) || presenterMap.get(channelId).isNullWebRtcEndpoint()) {
            String message = "No active sender now. Become sender or . Try again later ...";
            return Mono.just(convertService.convertObjectToJson(
                    Event.of(ResponseOperation.VIEWER_ACK, AnswerResponse.ofRejected(message), null)));
        }
        if (viewerMap.containsKey(channelId) && viewerMap.get(channelId).contains(userId)) {
            String message = "You are already viewing in this session. Use a different browser to add additional viewers.";
            return Mono.just(convertService.convertObjectToJson(
                    Event.of(ResponseOperation.VIEWER_ACK, AnswerResponse.ofRejected(message), null)));
        }
        Set<String> viewerSet = viewerMap.getOrDefault(channelId, new HashSet<>());
        viewerSet.add(userId);
        OfferRequest offerRequest = convertService.readDataFromMessage(receivedMessage, OfferRequest.class);
        UserSession presenterSession = presenterMap.get(channelId);
        WebRtcEndpoint nextWebRtc = presenterSession.createNextWebRtcEndpoint();
        nextWebRtc.addIceCandidateFoundListener(event -> session.send(createIceCandidateAck(session, event)).subscribe());
        UserSession userSession = userSessionMap.get(userId);
        userSession.addWebRtcEndpoint(nextWebRtc);
        presenterSession.connect(nextWebRtc);
        String sdpAnswer = nextWebRtc.processOffer(offerRequest.sdpOffer());
        return Mono.just(convertService.convertObjectToJson(
                        Event.of(ResponseOperation.VIEWER_ACK, AnswerResponse.ofAccepted(sdpAnswer), null)
                ))
                .doOnSuccess(ignored -> nextWebRtc.gatherCandidates());
    }

    private Mono<String> presenter(WebSocketSession session, String receivedMessage) {
        Long channelId = 1L;
        String userId = getUserIdFromSession(session);
        if (presenterMap.containsKey(channelId)) {
            String message = "Another user is currently acting as sender. Try again later ...";
            return Mono.just(convertService.convertObjectToJson(
                    Event.of(ResponseOperation.PRESENTER_ACK, AnswerResponse.ofRejected(message), null)));
        }
        UserSession presenterSession = presenterMap.computeIfAbsent(channelId, ignored -> {
            UserSession userSession = userSessionMap.get(userId);
            userSession.addMediaPipeline(kurento.createMediaPipeline());
            return userSession;
        });
        WebRtcEndpoint presenterWebRtc = presenterSession.getWebRtcEndpoint();
        presenterWebRtc.addIceCandidateFoundListener(event -> session.send(createIceCandidateAck(session, event)).subscribe());
        OfferRequest offerRequest = convertService.readDataFromMessage(receivedMessage, OfferRequest.class);
        String sdpAnswer = presenterWebRtc.processOffer(offerRequest.sdpOffer());
        return Mono.just(convertService.convertObjectToJson(
                        Event.of(ResponseOperation.PRESENTER_ACK, AnswerResponse.ofAccepted(sdpAnswer), null)
                ))
                .doOnSuccess(ignored -> presenterWebRtc.gatherCandidates());
    }

    private Mono<WebSocketMessage> createIceCandidateAck(WebSocketSession session, IceCandidateFoundEvent event) {
        return Mono.just(session.textMessage(convertService.convertObjectToJson(
                Event.of(ResponseOperation.ICE_CANDIDATE_ACK, CandidateResponse.of(event.getCandidate()), null)
        )));
    }

    public void cleanupResources(String userId) {
        log.info("Clean up Resources in ReplyHandler");
        disposeSubscriptionIfPresent(userId);
        removeUserSink(userId);
        removeActiveUserFromServer(userId)
                .flatMap(serverId -> voiceStateManager.removeVoiceState(Long.valueOf(serverId), userId))
                .subscribe();
    }

    /**
     * @param session
     * @param receivedMessage
     * @return jsonMessage
     * 클라이언트가 처음 연결하면 토큰으로 인증과정을 거치고 Heartbeat_interval 을 담아서 응답
     */
    private Flux<String> sendHello(WebSocketSession session, String receivedMessage) {
        String token = convertService.readDataFromMessage(receivedMessage, WebsocketAuthRequest.class).token();
        Event hello = Event.of(ResponseOperation.INIT_ACK,
                HelloResponse.of(serverProperties.getHeartbeatInterval()),
                null);
        return jwtTokenProvider.validateAndGetUserId(token)
                .doOnSuccess(userId -> initializeUserSink(userId, session))
                .thenMany(Flux.just(convertService.convertObjectToJson(hello)));
    }

    private void initializeUserSink(String userId, WebSocketSession session) {
        session.getAttributes().put("userId", userId);
        if (userSessionMap.containsKey(userId)) return;
        userSessionMap.computeIfAbsent(userId, ignored -> {
            Sinks.Many<String> userSink = Sinks.many().unicast().onBackpressureBuffer();
            activeUserManager.setSubscriptionRequired(userId, true);
            log.info("Create user sink : {}", userId);
            return UserSession.of(session, userSink);
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
        return userSessionMap.get(userId).getUserSinkAsFlux();
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
     * @param session
     * @param receivedMessage
     * @return jsonMessage
     * @apiNote 무조건 기존 서버 입장
     * 새로운 서버를 만들거나 새로운 서버에 초대된 경우 HTTP 를 사용하여 Server 참여 인원에 추가 (DB)
     */
    private Flux<String> enterServer(WebSocketSession session, String receivedMessage) {
        String userId = getUserIdFromSession(session);
        Long serverId = convertService.readDataFromMessage(receivedMessage, ServerRequest.class).serverId();
        return serverService.isValidServer(serverId)
                .filter(Boolean.TRUE::equals)
                .then(addActiveUser(userId, serverId))
                .then(userRepository.findByEmail(userId))
                .thenMany(createServerAck(serverId))
                .switchIfEmpty(Flux.error(new InvalidValueException(ErrorCode.INVALID_SERVER_ID, String.valueOf(serverId))));
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
        UserSession userSession = userSessionMap.get(userId);
        Disposable disposable = serverStreamManager.getMessageFromServerSink(serverId)
                .doOnNext(userSession::tryEmitNext)
                .subscribe(serverEvent -> log.info("ServerEvent emitted to {}: {}", userId, serverEvent));
        userSession.addSubscription(Subscription.of(serverId, disposable));
        log.info("[{}] subscribes server sink : {}", userId, serverId);
        activeUserManager.setSubscriptionRequired(userId, false);
    }

    private Flux<String> createServerAck(Long serverId) {
        return voiceStateManager.getAllVoiceState(serverId)
                .flatMap(this::createDataWithProfileImage)
                .switchIfEmpty(createServerAckEvent(EmptyResponse.of()));
    }

    private Flux<String> createDataWithProfileImage(Map.Entry<String, String> mapEntry) {
        VoiceState voiceState = convertService.convertJsonToObject(mapEntry.getValue(), VoiceState.class);
        return userRepository.findByEmail(voiceState.userId())
                .map(user -> ChannelEnterResponse.from(voiceState, user.getProfileImage()))
                .flatMapMany(this::createServerAckEvent);
    }

    private Flux<String> createServerAckEvent(Data response) {
        Event serverAck = Event.of(ResponseOperation.SERVER_ACK, response, null);
        return Flux.just(convertService.convertObjectToJson(serverAck));
    }

    /**
     * @param session
     * @param receivedMessage
     * @return empty
     * @apiNote 음성/영상 채널 입장 (채팅 제외)
     * 직접 소켓에 응답하지 않고 server_event 로 등록
     * TODO: Channel list에 포함되어 있는지 검사 (DB)
     */
    private Flux<String> enterChannel(WebSocketSession session, String receivedMessage) {
        String userId = getUserIdFromSession(session);
        ChannelRequest channelRequest = convertService.readDataFromMessage(receivedMessage, ChannelRequest.class);

        return isValidChannelId(channelRequest.serverId(), channelRequest.channelId())
                .flatMap(isValid -> isValid ?
                        Mono.empty() : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, String.valueOf(channelRequest.channelId()))))
                .then(activeUserManager.isCorrectAccess(userId, channelRequest.serverId()))
                .then(userRepository.findByEmail(userId))
                .flatMap(user -> createUserAndVoiceStateTuple(userId, user, channelRequest))
                .map(tuple -> ChannelEnterResponse.from(tuple.getT2(), tuple.getT1().getProfileImage()))
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
     * @param session
     * @param receivedMessage
     * @return empty
     * @apiNote 음성/영상 채널 나가기 (채팅 제외)
     * 직접 소켓에 응답하지 않고 server_event 로 등록
     */
    private Flux<String> leaveChannel(WebSocketSession session, String receivedMessage) {
        String userId = getUserIdFromSession(session);
        ChannelRequest channelRequest = convertService.readDataFromMessage(receivedMessage, ChannelRequest.class);
        return isValidChannelId(channelRequest.serverId(), channelRequest.channelId())
                .flatMap(isValid -> isValid ?
                        Mono.empty() : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, String.valueOf(channelRequest.channelId()))))
                .then(activeUserManager.isCorrectAccess(userId, channelRequest.serverId()))
                .then(deleteVoiceStateIfPresent(userId, channelRequest))
                .thenMany(putChannelLeaveToStream(userId, channelRequest));
    }

    // TODO: DB 연결되면 로직 추가
    private Mono<Boolean> isValidChannelId(Long ServerId, Long channelId) {
        List<Long> channalList = List.of(TEST_VIDEO_CHANNEL_ID, TEST_VOICE_CHANNEL_ID);
        return channalList.contains(channelId) ? Mono.just(true) : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, String.valueOf(channelId)));
    }

    private Mono<Long> deleteVoiceStateIfPresent(String userId, ChannelRequest channelRequest) {
        return voiceStateManager.removeVoiceState(channelRequest.serverId(), userId)
                .filter(result -> result == 1)
                .switchIfEmpty(Mono.error(new DuplicateOperationException(ErrorCode.DUPLICATE_CHANNEL_EXIT, String.valueOf(channelRequest.channelId()))));
    }

    private Mono<String> putChannelLeaveToStream(String userId, ChannelRequest channelRequest) {
        Event channelAck = Event.of(ResponseOperation.LEAVE_CHANNEL_EVENT, ChannelLeaveResponse.from(channelRequest, userId), null);
        String jsonChannelAck = convertService.convertObjectToJson(channelAck);
        return serverStreamManager.addVoiceMessageToStream(channelRequest.serverId(), jsonChannelAck)
                .doOnSuccess(ignored -> log.info("[{}] leaved the {} channel : id = {}", userId, channelRequest.channelType(), channelRequest.channelId()))
                .then(Mono.empty());
    }

    /**
     * @param session
     * @param receivedMessage
     * @return empty
     * @apiNote 음성/영상 채널의 상태(옵션) 변경 (채팅 제외)
     * 직접 소켓에 응답하지 않고 server_event 로 등록
     */
    private Flux<String> updateState(WebSocketSession session, String receivedMessage) {
        String userId = getUserIdFromSession(session);
        StateRequest stateRequest = convertService.readDataFromMessage(receivedMessage, StateRequest.class);

        return isValidChannelId(stateRequest.serverId(), stateRequest.channelId())
                .flatMap(isValid -> isValid ?
                        Mono.empty() : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, String.valueOf(stateRequest.channelId()))))
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
        UserSession userSession = userSessionMap.get(userId);
        if (userSession.isPresentSubscription()) {
            userSession.dispose();
            log.info("Dispose Subscription : {}", userId);
        }
    }

    private void removeUserSink(String userId) {
        userSessionMap.remove(userId);
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
