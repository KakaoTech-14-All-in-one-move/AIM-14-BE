package com.example.pitching.call.operation;

import com.example.pitching.call.service.ConvertService;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@ToString
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Room implements Closeable {
    @Getter
    private final Long channelId;
    private final MediaPipeline pipeline;
    private final ConcurrentMap<String, UserSession> participants = new ConcurrentHashMap<>();

    public static Room of(Long channelId, MediaPipeline pipeline) {
        return new Room(channelId, pipeline);
    }

    @PreDestroy
    private void shutdown() {
        this.close();
    }

    public UserSession join(String userName, WebSocketSession session, ConvertService convertService) {
        log.info("ROOM {}: adding participant {}", this.channelId, userName);
        final UserSession participant = UserSession.of(userName, this.channelId, session, this.pipeline);
        participant.addIceCandidateFoundListener(convertService);
        participants.put(participant.getUserId(), participant);
        return participant;
    }

    public void leave(UserSession user) {
        try {
            log.debug("PARTICIPANT {}: Leaving room {}", user.getUserId(), this.channelId);
            this.removeParticipant(user.getUserId());
            user.close();
        } catch (IOException e) {
            log.error("PARTICIPANT {}: Could not leave room {}", user.getUserId(), this.channelId, e);
        }
    }

    private void removeParticipant(String userId) throws IOException {
        participants.remove(userId);
        log.debug("ROOM {}: notifying all users that {} is leaving the room", this.channelId, userId);

        for (final UserSession participant : participants.values()) {
            participant.cancelVideoFrom(userId);
        }
    }

    public Collection<UserSession> getParticipants() {
        return participants.values();
    }


    @Override
    public void close() {
        for (final UserSession user : participants.values()) {
            try {
                user.close();
            } catch (IOException e) {
                log.debug("ROOM {}: Could not invoke close on participant {}", this.channelId, user.getUserId(), e);
            }
        }

        participants.clear();

        pipeline.release(new Continuation<>() {

            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("ROOM {}: Released Pipeline", Room.this.channelId);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("PARTICIPANT {}: Could not release Pipeline", Room.this.channelId);
            }
        });

        log.debug("Room {} closed", this.channelId);
    }
}
