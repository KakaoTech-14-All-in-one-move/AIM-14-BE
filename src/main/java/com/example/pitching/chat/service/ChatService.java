package com.example.pitching.chat.service;

import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.repository.ChatRepository;
import com.example.pitching.user.repository.ChannelRepository;
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

    public Mono<ChatMessage> saveTalkMessage(Long channelId, String sender, String senderName, String message, String profileImage) {
        ChatMessage chatMessage = ChatMessage.createTalkMessage(channelId, sender, senderName, message, profileImage);
        return chatRepository.save(chatMessage)
                .doOnSuccess(saved -> log.info("Saved message: {} for channel: {}", saved.getMessageId(), channelId))
                .doOnError(e -> log.error("Error saving message for channel {}: {}", channelId, e.getMessage()));
    }

    public Flux<ChatMessage> getChannelMessages(Long channelId) {
        if (channelId == null) {
            return Flux.error(new IllegalArgumentException("Channel ID cannot be null"));
        }

        return chatRepository.findByChannelIdOrderByTimestampAsc(channelId)
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