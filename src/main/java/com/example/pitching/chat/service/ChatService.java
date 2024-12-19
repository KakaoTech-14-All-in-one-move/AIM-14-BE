package com.example.pitching.chat.service;

import com.example.pitching.auth.domain.User;
import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.dto.UserUpdateMessage;
import com.example.pitching.chat.dto.ChatMessageDTO;
import com.example.pitching.chat.repository.ChatRepository;
import com.example.pitching.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    public Mono<ChatMessageDTO> saveTalkMessage(Long channelId, String sender, String message) {
        ChatMessage chatMessage = ChatMessage.createTalkMessage(channelId, sender, message);
        return chatRepository.save(chatMessage)
                .flatMap(savedMessage -> userRepository.findById(sender)
                        .map(user -> ChatMessageDTO.from(savedMessage, user)))
                .doOnSuccess(saved -> log.info("Saved message: {} for channel: {}", saved.getMessageId(), channelId))
                .doOnError(e -> log.error("Error saving message for channel {}: {}", channelId, e.getMessage()));
    }

    public Mono<ChatMessageDTO> createEnterMessage(Long channelId, String sender) {
        ChatMessage chatMessage = ChatMessage.createEnterMessage(channelId, sender);
        return chatRepository.save(chatMessage)
                .flatMap(savedMessage -> userRepository.findById(sender)
                        .map(user -> ChatMessageDTO.from(savedMessage, user)))
                .doOnSuccess(saved -> log.info("Saved enter message for channel: {}", channelId))
                .doOnError(e -> log.error("Error saving enter message for channel {}: {}", channelId, e.getMessage()));
    }

    public Mono<ChatMessageDTO> createLeaveMessage(Long channelId, String sender) {
        ChatMessage chatMessage = ChatMessage.createLeaveMessage(channelId, sender);
        return chatRepository.save(chatMessage)
                .flatMap(savedMessage -> userRepository.findById(sender)
                        .map(user -> ChatMessageDTO.from(savedMessage, user)))
                .doOnSuccess(saved -> log.info("Saved leave message for channel: {}", channelId))
                .doOnError(e -> log.error("Error saving leave message for channel {}: {}", channelId, e.getMessage()));
    }

    public Flux<ChatMessageDTO> getChannelMessages(Long channelId) {
        if (channelId == null) {
            return Flux.error(new IllegalArgumentException("Channel ID cannot be null"));
        }

        return chatRepository.findByChannelIdOrderByTimestampAsc(channelId)
                .flatMap(message -> userRepository.findById(message.getSender())
                        .map(user -> ChatMessageDTO.from(message, user)))
                .doOnSubscribe(s -> log.info("Starting to fetch messages for channel: {}", channelId))
                .doOnComplete(() -> log.info("Completed fetching messages for channel: {}", channelId))
                .doOnError(e -> log.error("Error fetching messages for channel {}: {}", channelId, e.getMessage()));
    }

    public Mono<Void> deleteChannelMessages(Long channelId) {
        return chatRepository.deleteByChannelId(channelId)
                .doOnSuccess(v -> log.info("Deleted all messages for channel: {}", channelId))
                .doOnError(e -> log.error("Error deleting messages for channel {}: {}", channelId, e.getMessage()));
    }
}