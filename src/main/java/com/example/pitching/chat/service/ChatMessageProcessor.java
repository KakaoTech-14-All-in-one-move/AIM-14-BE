package com.example.pitching.chat.service;

import com.example.pitching.chat.config.RabbitMQConfig;
import com.example.pitching.chat.dto.ChatMessageDTO;
import com.example.pitching.chat.handler.ChatWebSocketHandler;
import com.example.pitching.chat.repository.ChatRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageProcessor {
    private final ChatService chatService;
    private final ChatRedisRepository chatRedisRepository;
    private final ChatWebSocketHandler webSocketHandler;

    @RabbitListener(queues = RabbitMQConfig.CHAT_QUEUE)
    public void processMessage(ChatMessageDTO message) {
        log.info("Received message from queue: {}", message.getMessageId());

        // Save to DynamoDB
        Mono.just(message)
                .flatMap(msg -> chatService.saveMessageToDynamoDB(msg))
                // Save to Redis
                .flatMap(chatRedisRepository::saveMessage)
                // Broadcast to WebSocket
                .flatMap(saved -> webSocketHandler.broadcastToChannel(
                        message.getChannelId(), message))
                .doOnError(e -> log.error("Error processing message: {}", e.getMessage()))
                .subscribe();
    }
}