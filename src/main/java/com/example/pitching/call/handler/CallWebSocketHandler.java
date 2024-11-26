package com.example.pitching.call.handler;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.exception.CommonException;
import com.example.pitching.call.operation.Event;
import com.example.pitching.call.operation.response.ErrorResponse;
import com.example.pitching.call.service.ConvertService;
import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Slf4j
@NonNullApi
@Component
@RequiredArgsConstructor
public class CallWebSocketHandler implements WebSocketHandler {
    public static final String ANONYMOUS = "Anonymous";
    private final ServerProperties serverProperties;
    private final ConvertService convertService;
    private final ReplyHandler replyHandler;

    @Override
    public List<String> getSubProtocols() {
        return WebSocketHandler.super.getSubProtocols();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return replyMessages(session);
    }

    private Mono<Void> replyMessages(WebSocketSession session) {
        return session.send(
                session.receive()
                        .timeout(serverProperties.getTimeout())
                        .map(WebSocketMessage::getPayloadAsText)
                        .flatMap(jsonMessage -> replyHandler.handleMessages(session, jsonMessage)
                                .onErrorResume(e -> handleReplyErrors(getUserIdFromSession(session), e)))
                        .doOnNext(message -> log.info("[{}] Reply Message : {}", getUserIdFromSession(session), message))
                        .map(session::textMessage)
                        .doFinally(signalType -> {
                            String userId = getUserIdFromSession(session);
                            log.info("[{}] Disconnected: {}", userId, signalType);
                            if (!ANONYMOUS.equals(userId)) {
                                replyHandler.cleanupResources(userId);
                            }
                        })
        );
    }

    private Flux<String> handleReplyErrors(String userId, Throwable e) {
        if (!(e instanceof CommonException ex)) {
            log.error("Exception occurs in handling replyMessages : ", e);
            return Flux.error(e);
        }
        log.error("[{}] : {} -> ", userId, ex.getErrorCode().name(), ex);
        Event errorEvent = Event.error(ErrorResponse.from((CommonException) e));
        return Flux.just(convertService.convertObjectToJson(errorEvent));
    }

    private String getUserIdFromSession(WebSocketSession session) {
        return Optional.ofNullable(session.getAttributes().get("userId"))
                .map(Object::toString)
                .orElse(ANONYMOUS);
    }
}
