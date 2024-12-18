package com.example.pitching.chat.handler;

import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ChatService chatService;

    private final Map<Long, Map<String, WebSocketSession>> channelSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionSubscriptions = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.receive()
                .doOnSubscribe(sub -> log.info("New WebSocket connection: {}", session.getId()))
                .flatMap(message -> handleMessage(session, message))
                .doOnError(e -> log.error("WebSocket error: {}", e.getMessage()))
                .doFinally(signalType -> handleDisconnect(session))
                .then();
    }

    private Mono<Void> handleMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            WebSocketCommand command = objectMapper.readValue(message.getPayloadAsText(), WebSocketCommand.class);
            if (!isValidCommand(command)) {
                return handleError(session, new IllegalArgumentException("Invalid command type"));
            }

            return switch (command.getType()) {
                case "SUBSCRIBE" -> handleSubscribe(session, command);
                case "SEND" -> handleSend(session, command);
                case "UNSUBSCRIBE" -> handleUnsubscribe(session);
                default -> handleError(session, new IllegalArgumentException("Unknown message type"));
            };
        } catch (JsonProcessingException e) {
            return handleError(session, e);
        }
    }

    private Mono<Void> handleSubscribe(WebSocketSession session, WebSocketCommand command) {
        Long channelId = command.getChannelId();
        String sessionId = session.getId();

        channelSubscriptions.computeIfAbsent(channelId, k -> new ConcurrentHashMap<>())
                .put(sessionId, session);
        sessionSubscriptions.put(sessionId, channelId);

        log.info("Session {} subscribed to channel {}", sessionId, channelId);
        return Mono.empty();
    }

    private Mono<Void> handleSend(WebSocketSession session, WebSocketCommand command) {
        log.info("Received WebSocket command: type={}, channelId={}", command.getType(), command.getChannelId());
        ChatMessage chatMessage = command.getPayload().toChatMessage();

        return chatService.saveTalkMessage(
                        chatMessage.getChannelId(),
                        chatMessage.getSender(),
                        chatMessage.getSenderName(),
                        chatMessage.getMessage(),
                        chatMessage.getProfile_image()
                )
                .flatMap(saved -> broadcastToChannel(saved.getChannelId(), saved));
    }

    private Mono<Void> handleUnsubscribe(WebSocketSession session) {
        removeSession(session.getId());
        return Mono.empty();
    }

    private void handleDisconnect(WebSocketSession session) {
        String sessionId = session.getId();
        log.info("WebSocket disconnected: {}", sessionId);
        removeSession(sessionId);
    }


    public Mono<Void> closeChannelConnections(Long channelId) {
        Map<String, WebSocketSession> sessions = channelSubscriptions.get(channelId);
        if (sessions == null || sessions.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(sessions.values())
                .flatMap(session -> session.close()
                        .doOnError(e -> log.error("Error closing websocket session: {}", e.getMessage()))
                )
                .doFinally(signalType -> {
                    // 채널의 모든 세션 정보 정리
                    sessions.keySet().forEach(sessionId -> {
                        sessionSubscriptions.remove(sessionId);
                    });
                    channelSubscriptions.remove(channelId);
                })
                .then();
    }

    private void removeSession(String sessionId) {
        Long channelId = sessionSubscriptions.remove(sessionId);
        if (channelId != null) {
            Map<String, WebSocketSession> channelSessions = channelSubscriptions.get(channelId);
            if (channelSessions != null) {
                channelSessions.remove(sessionId);
                if (channelSessions.isEmpty()) {
                    channelSubscriptions.remove(channelId);
                }
            }
        }
    }

    private Mono<Void> broadcastToChannel(Long channelId, ChatMessage message) {
        Map<String, WebSocketSession> sessions = channelSubscriptions.get(channelId);
        if (sessions == null || sessions.isEmpty()) {
            return Mono.empty();
        }

        try {
            String payload = objectMapper.writeValueAsString(message);
            return Flux.fromIterable(sessions.values())
                    .flatMap(session -> session.send(Mono.just(session.textMessage(payload))))
                    .onErrorContinue((e, obj) -> log.error("Failed to send message: {}", e.getMessage()))
                    .then();
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    private Mono<Void> handleError(WebSocketSession session, Throwable e) {
        log.error("Error: {}", e.getMessage());
        return session.send(Mono.just(session.textMessage("Error: " + e.getMessage())));
    }

    private boolean isValidCommand(WebSocketCommand command) {
        return command != null &&
                command.getType() != null &&
                (command.getType().equals("SUBSCRIBE") ||
                        command.getType().equals("SEND") ||
                        command.getType().equals("UNSUBSCRIBE"));
    }
}
