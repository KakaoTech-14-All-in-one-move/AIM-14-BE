package com.example.pitching.call.handler;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.exception.CommonException;
import com.example.pitching.call.operation.Event;
import com.example.pitching.call.operation.response.ErrorResponse;
import com.example.pitching.call.service.ConvertService;
import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@NonNullApi
@Component
@RequiredArgsConstructor
public class CallWebSocketHandler implements WebSocketHandler {
    private final ServerProperties serverProperties;
    private final ConvertService convertService;
    private final ReplyHandler replyHandler;

    @Override
    public List<String> getSubProtocols() {
        return WebSocketHandler.super.getSubProtocols();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return getUserIdFromContext()
                .flatMap(userId ->
                        replyHandler.initializeUserSink(userId)
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
                                        .flatMap(jsonMessage -> replyHandler.handleMessages(jsonMessage, userId)
                                                .onErrorResume(e -> handleErrors(userId, e)))
                                        .doOnNext(message -> log.info("[{}] Reply Message : {}", userId, message))
                                        .map(session::textMessage)
                                        .doFinally(signalType -> {
                                            log.info("[{}] Disconnected: {}", userId, signalType);
                                            replyHandler.removeUserSink(userId);
                                            replyHandler.disposeSubscription(userId);
                                        })
                        )
        );
    }

    private Mono<Void> sendMessages(WebSocketSession session, Mono<String> cachedUserIdMono) {
        return session.send(
                cachedUserIdMono
                        .flatMapMany(userId ->
                                replyHandler.getMessageFromUserSink(userId)
                                        .doOnNext(message -> log.info("[{}] Server Message : {}", userId, message))
                                        .doOnError(error -> log.error("Error occurs in handling Server Message()", error))
                                        .map(session::textMessage)
                        )
        );
    }

    private Flux<String> handleErrors(String userId, Throwable e) {
        if (!(e instanceof CommonException ex)) return Flux.error(e);
        log.error("[{}] : {} -> {}", userId, ex.getErrorCode().name(), ex.getValue());
        Event errorEvent = Event.error(ErrorResponse.from((CommonException) e));
        return Flux.just(convertService.convertObjectToJson(errorEvent));
    }

    private Mono<String> getUserIdFromContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> (UserDetails) context.getAuthentication().getPrincipal())
                .doOnNext(userDetails -> log.info("UserDetails: {}", userDetails))
                .map(UserDetails::getUsername)
                .cache();
    }
}
