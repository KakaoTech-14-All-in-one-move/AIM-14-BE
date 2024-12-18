package com.example.pitching.chat.service;

import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.repository.ChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @InjectMocks
    private ChatService chatService;

    private ChatMessage sampleChatMessage;
    private final Long channelId = 1L;
    private final String sender = "testUser";
    private final String senderName = "Test User";
    private final String message = "Hello, World!";
    private final String profileImage = "profile.jpg";

    @BeforeEach
    void setUp() {
        sampleChatMessage = ChatMessage.createTalkMessage(
                channelId, sender, senderName, message, profileImage
        );
    }

    @Test
    void saveTalkMessage_Success() {
        when(chatRepository.save(any(ChatMessage.class)))
                .thenReturn(Mono.just(sampleChatMessage));

        StepVerifier.create(chatService.saveTalkMessage(
                        channelId, sender, senderName, message, profileImage))
                .expectNext(sampleChatMessage)
                .verifyComplete();

        verify(chatRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void saveTalkMessage_Error() {
        when(chatRepository.save(any(ChatMessage.class)))
                .thenReturn(Mono.error(new RuntimeException("DB Error")));

        StepVerifier.create(chatService.saveTalkMessage(
                        channelId, sender, senderName, message, profileImage))
                .expectError(RuntimeException.class)
                .verify();

        verify(chatRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void getChannelMessages_Success() {
        when(chatRepository.findByChannelIdOrderByTimestampAsc(channelId))
                .thenReturn(Flux.just(sampleChatMessage));

        StepVerifier.create(chatService.getChannelMessages(channelId))
                .expectNext(sampleChatMessage)
                .verifyComplete();

        verify(chatRepository, times(1))
                .findByChannelIdOrderByTimestampAsc(channelId);
    }

    @Test
    void getChannelMessages_WithNullChannelId() {
        StepVerifier.create(chatService.getChannelMessages(null))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(chatRepository, never())
                .findByChannelIdOrderByTimestampAsc(any());
    }

    @Test
    void deleteChannelMessages_Success() {
        when(chatRepository.deleteByChannelId(channelId))
                .thenReturn(Mono.empty());

        StepVerifier.create(chatService.deleteChannelMessages(channelId))
                .verifyComplete();

        verify(chatRepository, times(1)).deleteByChannelId(channelId);
    }

    @Test
    void deleteChannelMessages_Error() {
        when(chatRepository.deleteByChannelId(channelId))
                .thenReturn(Mono.error(new RuntimeException("Delete Error")));

        StepVerifier.create(chatService.deleteChannelMessages(channelId))
                .expectError(RuntimeException.class)
                .verify();

        verify(chatRepository, times(1)).deleteByChannelId(channelId);
    }
}