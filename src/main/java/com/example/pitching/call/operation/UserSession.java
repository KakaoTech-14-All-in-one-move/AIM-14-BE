package com.example.pitching.call.operation;

import com.example.pitching.call.dto.Subscription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@ToString
@AllArgsConstructor
public class UserSession {
    private WebSocketSession session;
    private Sinks.Many<String> userSink;
    private Subscription subscription;
    private MediaPipeline mediaPipeline;
    @Getter
    private WebRtcEndpoint webRtcEndpoint;

    public static UserSession of(WebSocketSession session, Sinks.Many<String> userSink) {
        return new UserSession(session, userSink, null, null, null);
    }

    public void addSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public void addMediaPipeline(MediaPipeline mediaPipeline) {
        this.mediaPipeline = mediaPipeline;
        this.webRtcEndpoint = new WebRtcEndpoint.Builder(mediaPipeline).build();
    }

    public void addWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint) {
        this.webRtcEndpoint = webRtcEndpoint;
    }

    public WebRtcEndpoint createNextWebRtcEndpoint() {
        return new WebRtcEndpoint.Builder(mediaPipeline).build();
    }

    public void addCandidate(IceCandidate candidate) {
        this.webRtcEndpoint.addIceCandidate(candidate);
    }

    public boolean isSameSession(WebSocketSession session) {
        return this.session.getId().equals(session.getId());
    }

    public boolean isNullWebRtcEndpoint() {
        return this.webRtcEndpoint == null;
    }

    public boolean isPresentSubscription() {
        return this.subscription != null;
    }

    public void connect(WebRtcEndpoint nextWebRtc) {
        this.webRtcEndpoint.connect(nextWebRtc);
    }

    public void sendMessage(String message) {
        this.session.send(Mono.just(session.textMessage(message)))
                .subscribeOn(Schedulers.single()).subscribe();
    }

    public Flux<String> getUserSinkAsFlux() {
        return this.userSink.asFlux();
    }

    public void tryEmitNext(String message) {
        this.userSink.tryEmitNext(message);
    }

    public void dispose() {
        if (subscription != null) {
            subscription.disposable().dispose();
        }
    }

    public void releaseMediaPipeline() {
        if (mediaPipeline != null) {
            mediaPipeline.release();
            mediaPipeline = null;
        }
    }

    public void releaseWebRtcEndpoint() {
        if (webRtcEndpoint != null) {
            webRtcEndpoint.release();
        }
    }
}
