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
import com.example.pitching.call.operation.response.ChannelEnterResponse;
import com.example.pitching.call.operation.response.ChannelLeaveResponse;
import com.example.pitching.call.operation.response.EmptyResponse;
import com.example.pitching.call.operation.response.HelloResponse;
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

    public Flux<String> handleMessages(String receivedMessage, String userId) {
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

    public void subscribeServerSink(String serverId, String userId) {
        disposeSubscription(userId);
        Sinks.Many<String> userSink = userSinkMap.get(userId);
        Disposable disposable = serverStreamManager.getMessageFromServerSink(serverId)
                .doOnNext(userSink::tryEmitNext)
                .subscribe();
        userSubscription.put(userId, Subscription.of(serverId, disposable));
        log.info("[{}] subscribes server sink : {}", userId, serverId);
    }

    public void disposeSubscription(String userId) {
        if (userSubscription.containsKey(userId)) {
            userSubscription.remove(userId).dispose();
            log.info("Dispose Subscription : {}", userId);
        }
    }

    public void removeUserSink(String userId) {
        userSinkMap.remove(userId);
    }

    public void removeActiveUserFromServer(String userId) {
        activeUserManager.removeActiveUser(userId).subscribe();
    }

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
        return isValidServerId(serverId)
                .filter(Boolean.TRUE::equals)
                .then(addActiveUser(userId, serverId))
                .thenMany(createServerAck(serverId))
                .switchIfEmpty(Flux.error(new InvalidValueException(ErrorCode.INVALID_SERVER_ID, serverId)));
    }

    private Mono<Boolean> addActiveUser(String userId, String serverId) {
        return activeUserManager.addActiveUser(userId, serverId)
                .filter(Boolean.TRUE::equals)
                .doOnSuccess(ignored -> subscribeServerSink(serverId, userId));
    }

    private Flux<String> createServerAck(String serverId) {
        return voiceStateManager.getAllVoiceState(serverId)
                .map(mapEntry -> ChannelEnterResponse.from(convertService.convertJsonToData(mapEntry.getValue(), VoiceState.class)))
                .flatMap(this::createServerAckEvent)
                .switchIfEmpty(createServerAckEvent(EmptyResponse.of()));
    }

    // TODO: Server list에 포함되어 있는지 검사 (DB)
    private Mono<Boolean> isValidServerId(String serverId) {
        return Mono.just(true);
    }

    private Flux<String> createServerAckEvent(Data response) {
        Event serverAck = Event.of(ResponseOperation.SERVER_ACK, response, null);
        return Flux.just(convertService.convertObjectToJson(serverAck));
    }

    private Flux<String> enterChannel(String receivedMessage, String userId) {
        ChannelRequest channelRequest = convertService.readDataFromMessage(receivedMessage, ChannelRequest.class);
        // TODO: userId로 username + Image Url 조회 (DB)
        String username = "name";

        String jsonVoiceState = convertService.convertObjectToJson(VoiceState.from(channelRequest, userId, username));
        return isValidChannelId(channelRequest.serverId(), channelRequest.channelId())
                .flatMap(isValid -> isValid ?
                        Mono.empty() : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, channelRequest.channelId())))
                .then(activeUserManager.isCorrectAccess(userId, channelRequest.serverId()))
                .then(saveAndGetVoiceState(userId, channelRequest, jsonVoiceState))
                .flatMapMany(voiceState -> putChannelEnterToStream(userId, voiceState, channelRequest));
    }

    private Mono<VoiceState> saveAndGetVoiceState(String userId, ChannelRequest channelRequest, String jsonVoiceState) {
        return voiceStateManager.addIfAbsentOrChangeChannel(channelRequest, userId, jsonVoiceState)
                .then(voiceStateManager.getVoiceState(channelRequest.serverId(), userId))
                .flatMap(convertService::convertJsonToVoiceState);
    }

    private Mono<String> putChannelEnterToStream(String userId, VoiceState voiceState, ChannelRequest channelRequest) {
        Event channelAck = Event.of(ResponseOperation.ENTER_CHANNEL_ACK, ChannelEnterResponse.from(voiceState), null);
        String jsonChannelAck = convertService.convertObjectToJson(channelAck);
        return serverStreamManager.addVoiceMessageToStream(channelRequest.serverId(), jsonChannelAck)
                .doOnSuccess(record -> log.info("[{}] entered the {} channel : id = {}", userId, channelRequest.channelType(), channelRequest.channelId()))
                .then(Mono.empty());
    }

    // TODO: Channel list에 포함되어 있는지 검사 (DB)
    private Mono<Boolean> isValidChannelId(String ServerId, String channelId) {
        return Mono.just(true);
    }

    private Flux<String> leaveChannel(String receivedMessage, String userId) {
        ChannelRequest channelRequest = convertService.readDataFromMessage(receivedMessage, ChannelRequest.class);
        return isValidChannelId(channelRequest.serverId(), channelRequest.channelId())
                .flatMap(isValid -> isValid ?
                        Mono.empty() : Mono.error(new InvalidValueException(ErrorCode.INVALID_CHANNEL_ID, channelRequest.channelId())))
                .then(activeUserManager.isCorrectAccess(userId, channelRequest.serverId()))
                .then(deleteVoiceStateIfPresent(userId, channelRequest))
                .thenMany(putChannelLeaveToStream(userId, channelRequest));
    }

    private Mono<Long> deleteVoiceStateIfPresent(String userId, ChannelRequest channelRequest) {
        return voiceStateManager.removeVoiceState(channelRequest.serverId(), userId)
                .filter(result -> result == 1)
                .switchIfEmpty(Mono.error(new DuplicateOperationException(ErrorCode.DUPLICATE_CHANNEL_EXIT, channelRequest.channelId())));
    }

    private Mono<String> putChannelLeaveToStream(String userId, ChannelRequest channelRequest) {
        Event channelAck = Event.of(ResponseOperation.LEAVE_CHANNEL_ACK, ChannelLeaveResponse.from(channelRequest, userId), null);
        String jsonChannelAck = convertService.convertObjectToJson(channelAck);
        return serverStreamManager.addVoiceMessageToStream(channelRequest.serverId(), jsonChannelAck)
                .doOnSuccess(record -> log.info("[{}] leaved the {} channel : id = {}", userId, channelRequest.channelType(), channelRequest.channelId()))
                .then(Mono.empty());
    }
}
