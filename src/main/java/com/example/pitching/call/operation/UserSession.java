package com.example.pitching.call.operation;

import com.example.pitching.call.operation.code.ResponseOperation;
import com.example.pitching.call.operation.response.AnswerResponse;
import com.example.pitching.call.operation.response.CandidateResponse;
import com.example.pitching.call.service.ConvertService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@ToString
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSession implements Closeable {
    @Getter
    private final String userId;
    @Getter
    private final Long channelId;
    @Getter
    private final WebSocketSession session;
    private final MediaPipeline pipeline;
    private final WebRtcEndpoint outgoingMedia;
    private final ConcurrentMap<String, WebRtcEndpoint> incomingMedia = new ConcurrentHashMap<>();

    public static UserSession of(String userId, Long channelId, WebSocketSession session, MediaPipeline pipeline) {
        return new UserSession(userId, channelId, session, pipeline, new WebRtcEndpoint.Builder(pipeline).build());
    }

    public void addIceCandidateFoundListener(ConvertService convertService) {
        this.outgoingMedia.addIceCandidateFoundListener(event -> {
            Event response = Event.of(ResponseOperation.ICE_CANDIDATE, CandidateResponse.of(userId, event.getCandidate()), null);
            sendMessage(convertService.convertObjectToJson(response));
        });
    }

    public WebRtcEndpoint getOutgoingWebRtcPeer() {
        return outgoingMedia;
    }

    public void sendMessage(String message) {
        this.session.send(Mono.just(session.textMessage(message)))
                .subscribeOn(Schedulers.single()).subscribe();
    }


    public void addCandidate(IceCandidate candidate, String userId) {
        if (this.userId.compareTo(userId) == 0) {
            outgoingMedia.addIceCandidate(candidate);
        } else {
            WebRtcEndpoint webRtc = incomingMedia.get(userId);
            if (webRtc != null) {
                webRtc.addIceCandidate(candidate);
            }
        }
    }

    public void receiveVideoFrom(UserSession sender, String sdpOffer, ConvertService convertService) {
        log.info("USER [{}]: connecting with {} in room {}", this.userId, sender.getUserId(), this.channelId);

        try {
            final WebRtcEndpoint endpoint = this.getEndpointForUser(sender, convertService);
            if (endpoint == null) {
                log.error("USER [{}]: Error creating endpoint for {}", this.userId, sender.getUserId());
                return;
            }

            // 상태 체크 추가
            ConnectionState state = endpoint.getConnectionState();
            log.info("USER [{}]: Current endpoint state for {}: {}",
                    this.userId, sender.getUserId(), state);

            // 연결 재설정이 필요한 경우
            if (state != ConnectionState.CONNECTED) {
                endpoint.gatherCandidates(); // ICE 후보 재수집
            }

            log.warn("USER [{}]: Request receiving video from {}", this.userId, sender.getUserId());
            final String ipSdpAnswer = endpoint.processOffer(sdpOffer);
            Event response = Event.of(ResponseOperation.RECEIVE_VIDEO_ANSWER,
                    AnswerResponse.of(sender.userId, ipSdpAnswer), null);

            this.sendMessage(convertService.convertObjectToJson(response));

            // ICE candidate 수집 완료 이벤트 추가
            endpoint.addIceGatheringDoneListener(event -> {
                log.info("USER [{}]: ICE gathering done for {}", this.userId, sender.getUserId());
                // 추가 처리 필요시
            });

        } catch (Exception e) {
            log.error("USER [{}]: Error receiving video from {}", this.userId, sender.getUserId(), e);
        }
    }

    private WebRtcEndpoint getEndpointForUser(final UserSession sender, ConvertService convertService) {
        if (sender.getUserId().equals(userId)) {
            log.debug("PARTICIPANT {}: configuring loopback", this.userId);
            return outgoingMedia;
        }

        log.debug("PARTICIPANT {}: receiving video from {}", this.userId, sender.getUserId());

        WebRtcEndpoint incoming = incomingMedia.get(sender.getUserId());
        if (incoming == null) {
            try {
                log.debug("PARTICIPANT {}: creating new endpoint for {}", this.userId, sender.getUserId());
                incoming = new WebRtcEndpoint.Builder(pipeline).build();

                incoming.addIceCandidateFoundListener(event -> {
                    Event response = Event.of(ResponseOperation.ICE_CANDIDATE,
                            CandidateResponse.of(sender.getUserId(), event.getCandidate()), null);
                    sendMessage(convertService.convertObjectToJson(response));
                });

                incomingMedia.put(sender.getUserId(), incoming);
                sender.getOutgoingWebRtcPeer().connect(incoming);
                log.warn("incomingMedia: {}", incomingMedia);
            } catch (Exception e) {
                log.error("PARTICIPANT {}: Error creating endpoint for {}", this.userId, sender.getUserId(), e);
                return null;
            }
        }

        return incoming;
    }

    public void cancelVideoFrom(final UserSession sender) {
        this.cancelVideoFrom(sender.getUserId());
    }

    public void cancelVideoFrom(final String senderName) {
        log.debug("PARTICIPANT {}: canceling video reception from {} (current endpoints: {})",
                this.userId,
                senderName,
                incomingMedia.keySet()  // 현재 저장된 모든 키를 로그로 출력
        );

        final WebRtcEndpoint incoming = incomingMedia.remove(senderName);

        if (incoming == null) {
            log.warn("PARTICIPANT {}: no incoming endpoint found for {} (remaining endpoints: {})",
                    this.userId,
                    senderName,
                    incomingMedia.keySet()  // 제거 후 남은 키들을 로그로 출력
            );
            return;
        }

        incoming.release(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("PARTICIPANT {}: Released successfully incoming EP for {}",
                        UserSession.this.userId, senderName);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("PARTICIPANT {}: Could not release incoming EP for {}", UserSession.this.userId,
                        senderName);
            }
        });
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (!(obj instanceof UserSession)) {
            return false;
        }
        UserSession other = (UserSession) obj;
        boolean eq = userId.equals(other.userId);
        eq &= channelId.equals(other.channelId);
        return eq;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + userId.hashCode();
        result = 31 * result + channelId.hashCode();
        return result;
    }

    @Override
    public void close() throws IOException {
        log.debug("PARTICIPANT {}: Releasing resources", this.userId);
        for (final String remoteParticipantName : incomingMedia.keySet()) {
            log.trace("PARTICIPANT {}: Released incoming EP for {}", this.userId, remoteParticipantName);

            final WebRtcEndpoint ep = this.incomingMedia.get(remoteParticipantName);

            ep.release(new Continuation<>() {

                @Override
                public void onSuccess(Void result) throws Exception {
                    log.trace("PARTICIPANT {}: Released successfully incoming EP for {}",
                            UserSession.this.userId, remoteParticipantName);
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.warn("PARTICIPANT {}: Could not release incoming EP for {}", UserSession.this.userId,
                            remoteParticipantName);
                }
            });
        }

        outgoingMedia.release(new Continuation<>() {

            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("PARTICIPANT {}: Released outgoing EP", UserSession.this.userId);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("USER {}: Could not release outgoing EP", UserSession.this.userId);
            }
        });
    }
}
