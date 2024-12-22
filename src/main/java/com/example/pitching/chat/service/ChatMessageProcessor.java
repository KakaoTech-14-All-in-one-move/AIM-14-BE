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

        Mono.just(message)
                .flatMap(msg -> chatService.saveMessageToDynamoDB(msg))
                .flatMap(savedMsg ->
                        // 각 작업을 독립적으로 처리
                        chatRedisRepository.saveMessage(savedMsg)
                                .onErrorResume(e -> {
                                    log.error("Failed to save to Redis: {}", e.getMessage(), e);
                                    return Mono.empty(); // Redis 실패해도 계속 진행
                                })
                                .then(webSocketHandler.broadcastToChannel(message.getChannelId(), message)
                                        .onErrorResume(e -> {
                                            log.error("Failed to broadcast: {}", e.getMessage(), e);
                                            return Mono.empty(); // 브로드캐스트 실패해도 계속 진행
                                        })
                                )
                )
                .doOnError(e -> {
                    log.error("Critical error in message processing: {}",
                            message.getMessageId(),
                            e
                    );
                })
                .subscribe();
    }
}