package com.example.pitching.chat.service;

import com.example.pitching.auth.domain.User;
import com.example.pitching.chat.config.RabbitMQConfig;
import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.dto.ChatMessageDTO;
import com.example.pitching.chat.repository.ChatRepository;
import com.example.pitching.chat.repository.ChatRedisRepository;
import com.example.pitching.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatRedisRepository chatRedisRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    public Mono<ChatMessageDTO> saveTalkMessage(Long channelId, String sender, String message) {
        ChatMessage chatMessage = ChatMessage.createTalkMessage(channelId, sender, message);
        return userRepository.findById(sender)
                .map(user -> ChatMessageDTO.from(chatMessage, user))
                .doOnNext(messageDTO -> {
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.CHAT_EXCHANGE,
                            RabbitMQConfig.CHAT_ROUTING_KEY,
                            messageDTO
                    );
                })
                .doOnSuccess(saved -> log.info("Message sent to queue: {}", saved.getMessageId()))
                .doOnError(e -> log.error("Error sending message to queue: {}", e.getMessage()));
    }

    public Mono<ChatMessageDTO> saveMessageToDynamoDB(ChatMessageDTO messageDTO) {
        ChatMessage chatMessage = new ChatMessage(
                messageDTO.getChannelId(),
                messageDTO.getTimestamp(),
                messageDTO.getMessageId(),
                messageDTO.getType(),
                messageDTO.getSender(),
                messageDTO.getMessage()
        );

        log.info("Attempting to save ChatMessage to DynamoDB: channelId={}, messageId={}",
                chatMessage.getChannelId(), chatMessage.getMessageId());

        return chatRepository.save(chatMessage)
                .doOnSuccess(savedMessage ->
                        log.info("Successfully saved message to DynamoDB: messageId={}",
                                savedMessage.getMessageId()))
                .doOnError(e ->
                        log.error("Failed to save message to DynamoDB: messageId={}, error={}",
                                chatMessage.getMessageId(), e.getMessage(), e))
                .thenReturn(messageDTO);
    }

    public Flux<ChatMessageDTO> getChannelMessages(Long channelId) {
        return chatRedisRepository.getRecentMessages(channelId)
                .switchIfEmpty(
                        chatRepository.findByChannelIdOrderByTimestampAsc(channelId)
                                .flatMap(message -> userRepository.findById(message.getSender())
                                        .map(user -> ChatMessageDTO.from(message, user)))
                                .flatMap(msg -> chatRedisRepository.saveMessage(msg).thenReturn(msg))
                )
                .doOnSubscribe(s -> log.info("Fetching messages for channel: {}", channelId))
                .doOnComplete(() -> log.info("Completed fetching messages for channel: {}", channelId))
                .doOnError(e -> log.error("Error fetching messages for channel {}: {}", channelId, e.getMessage()));
    }

    public Mono<Void> deleteChannelMessages(Long channelId) {
        return Mono.when(
                        chatRepository.deleteByChannelId(channelId),
                        chatRedisRepository.deleteChannelMessages(channelId)
                ).doOnSuccess(v -> log.info("Deleted all messages for channel: {}", channelId))
                .doOnError(e -> log.error("Error deleting messages for channel {}: {}", channelId, e.getMessage()));
    }
}