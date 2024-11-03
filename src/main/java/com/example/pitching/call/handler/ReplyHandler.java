package com.example.pitching.call.handler;

import com.example.pitching.call.dto.Subscription;
import com.example.pitching.call.dto.VoiceState;
import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.exception.DuplicateOperationException;
import com.example.pitching.call.exception.ErrorCode;
import com.example.pitching.call.exception.InvalidValueException;
import com.example.pitching.call.operation.Data;
import com.example.pitching.call.operation.Event;
import com.example.pitching.call.operation.code.RequestOperation;
import com.example.pitching.call.operation.code.ResponseOperation;
import com.example.pitching.call.operation.request.ChannelRequest;
import com.example.pitching.call.operation.request.ServerRequest;
import com.example.pitching.call.operation.request.StateRequest;
import com.example.pitching.call.operation.response.ChannelLeaveResponse;
import com.example.pitching.call.operation.response.ChannelResponse;
import com.example.pitching.call.operation.response.EmptyResponse;
import com.example.pitching.call.operation.response.HelloResponse;
import com.example.pitching.call.server.CallUdpClient;
import com.example.pitching.call.service.ActiveUserManager;
import com.example.pitching.call.service.ConvertService;
import com.example.pitching.call.service.ServerStreamManager;
import com.example.pitching.call.service.VoiceStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplyHandler {
    private final Map<String, Sinks.Many<String>> userSinkMap = new ConcurrentHashMap<>();
    private final Map<String, Subscription> userSubscription = new ConcurrentHashMap<>();
    private final ServerProperties serverProperties;
    private final ConvertService convertService;
    private final ServerStreamManager serverStreamManager;
    private final VoiceStateManager voiceStateManager;
    private final ActiveUserManager activeUserManager;
    private final CallUdpClient callUdpClient;

    public Mono<Void> initializeUserSink(String userId) {
        return Mono.fromRunnable(() -> {
            Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
            userSinkMap.put(userId, sink);
            log.info("Create user sink : {}", userId);
        });
    }

    public Flux<String> getMessageFromUserSink(String userId) {
        return userSinkMap.get(userId).asFlux();
    }

    public Flux<String> handleMessages(String receivedMessage, String userId) {
        return convertService.readRequestOperationFromMessage(receivedMessage)
                .doOnSuccess(requestOperation -> log.info("[{}] Send Message : {} -> RequestOperation : {}", userId, receivedMessage, requestOperation))
                .flatMapMany(requestOperation -> switch (requestOperation) {
                    case RequestOperation.INIT -> sendHello();
                    case RequestOperation.HEARTBEAT -> sendHeartbeatAck();
                    case RequestOperation.SERVER -> enterServer(receivedMessage, userId);
                    case RequestOperation.ENTER_CHANNEL -> enterChannel(receivedMessage, userId);
                    case RequestOperation.LEAVE_CHANNEL -> leaveChannel(receivedMessage, userId);
                    case RequestOperation.UPDATE_STATE -> updateState(receivedMessage, userId);
                });
    }

    public void cleanupResources(String userId) {
        disposeSubscription(userId);
        removeUserSink(userId);
        removeActiveUserFromServer(userId)
                .flatMap(serverId -> voiceStateManager.removeVoiceState(serverId, userId))
                .subscribe();
    }

    /**
     * @return jsonMessage
     * 클라이언트가 처음 연결하면 Heartbeat_interval 을 담아서 응답
     */
    private Flux<String> sendHello() {
        Event hello = Event.of(ResponseOperation.INIT_ACK,
                HelloResponse.of(serverProperties.getHeartbeatInterval()),
                null);
        return Flux.just(convertService.convertObjectToJson(hello));
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
        return Mono.just(true);
    }

    private Mono<Boolean> addActiveUser(String userId, String serverId) {
        return activeUserManager.addActiveUser(userId, serverId)
                .filter(Boolean.TRUE::equals)
                .doOnSuccess(_ -> subscribeServerSink(serverId, userId));
    }

    private void subscribeServerSink(String serverId, String userId) {
        disposeSubscription(userId);
        Sinks.Many<String> userSink = userSinkMap.get(userId);
        Disposable disposable = serverStreamManager.getMessageFromServerSink(serverId)
                .doOnNext(userSink::tryEmitNext)
                .subscribe();
        userSubscription.put(userId, Subscription.of(serverId, disposable));
        log.info("[{}] subscribes server sink : {}", userId, serverId);
    }

    private Flux<String> createServerAck(String serverId) {
        return voiceStateManager.getAllVoiceState(serverId)
                .map(mapEntry -> ChannelResponse.from(convertService.convertJsonToData(mapEntry.getValue(), VoiceState.class)))
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
        // TODO: userId로 username + Image Url 조회 (DB)
        String username = "name";

        String jsonVoiceState = convertService.convertObjectToJson(VoiceState.from(channelRequest, userId, username));
        return isValidChannelId(channelRequest.serverId(), channelRequest.channelId())
                .flatMap(isValid -> isValid ?
                        Mono.empty() : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, channelRequest.channelId())))
                .then(activeUserManager.isCorrectAccess(userId, channelRequest.serverId()))
                .then(updateChannelAndGetVoiceState(userId, channelRequest, jsonVoiceState))
                .doOnSuccess(callUdpClient::initializeChannelSink)
                .flatMapMany(voiceState -> putChannelEnterToStream(userId, voiceState));
    }

    private Mono<VoiceState> updateChannelAndGetVoiceState(String userId, ChannelRequest channelRequest, String jsonVoiceState) {
        return voiceStateManager.addIfAbsentOrChangeChannel(channelRequest, userId, jsonVoiceState)
                .then(voiceStateManager.getVoiceState(channelRequest.serverId(), userId))
                .flatMap(convertService::convertJsonToVoiceState);
    }

    private Mono<String> putChannelEnterToStream(String userId, VoiceState voiceState) {
        Event channelAck = Event.of(ResponseOperation.ENTER_CHANNEL_EVENT, ChannelResponse.from(voiceState), null);
        String jsonChannelAck = convertService.convertObjectToJson(channelAck);
        return serverStreamManager.addVoiceMessageToStream(voiceState.serverId(), jsonChannelAck)
                .doOnSuccess(record -> log.info("[{}] entered the {} channel : id = {}", userId, voiceState.channelId(), voiceState.channelId()))
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
                .thenMany(putChannelLeaveToStream(userId, channelRequest));
    }

    // TODO: DB 연결되면 로직 추가
    private Mono<Boolean> isValidChannelId(String ServerId, String channelId) {
        return Mono.just(true);
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
                .doOnSuccess(record -> log.info("[{}] leaved the {} channel : id = {}", userId, channelRequest.channelType(), channelRequest.channelId()))
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
        Event stateAck = Event.of(ResponseOperation.UPDATE_STATE_EVENT, ChannelResponse.from(voiceState), null);
        String jsonStateAck = convertService.convertObjectToJson(stateAck);
        return serverStreamManager.addVoiceMessageToStream(voiceState.serverId(), jsonStateAck)
                .doOnSuccess(record -> log.info("[{}] updated the state : id = {}", userId, stateRequest))
                .then(Mono.empty());
    }

    private void disposeSubscription(String userId) {
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
        return activeUserManager.removeActiveUser(userId);
    }
}
