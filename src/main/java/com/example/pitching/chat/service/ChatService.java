package com.example.pitching.chat.service;

import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;

    public Mono<ChatMessage> saveMessage(String channelId, ChatMessage.MessageType type,
                                         String sender, String content, String profileImage) {
        ChatMessage message = ChatMessage.createChatMessage(
                channelId, type, sender, content, profileImage);

        message.setMessageId(UUID.randomUUID().toString());
        message.setTimestamp(LocalDateTime.now().toString());

        return chatMessageRepository.save(message);
    }

    public Flux<ChatMessage> getChannelMessages(String channelId) {
        return chatMessageRepository.findByChannelId(channelId);
    }
}
