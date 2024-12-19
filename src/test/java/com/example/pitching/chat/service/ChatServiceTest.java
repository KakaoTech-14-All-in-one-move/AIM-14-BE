package com.example.pitching.chat.service;

import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.dto.ChatMessageDTO;
import com.example.pitching.chat.repository.ChatRepository;
import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.auth.domain.User;
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

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    private ChatMessage sampleChatMessage;
    private User sampleUser;
    private final Long channelId = 1L;
    private final String sender = "test@example.com";
    private final String username = "Test User";
    private final String message = "Hello, World!";
    private final String profileImage = "profile.jpg";

    @BeforeEach
    void setUp() {
        sampleChatMessage = ChatMessage.createTalkMessage(
                channelId, sender, message
        );

        sampleUser = User.createNewUser(sender, username, profileImage, "password");

        when(userRepository.findById(sender))
                .thenReturn(Mono.just(sampleUser));
    }

    @Test
    void saveTalkMessage_Success() {
        when(chatRepository.save(any(ChatMessage.class)))
                .thenReturn(Mono.just(sampleChatMessage));

        StepVerifier.create(chatService.saveTalkMessage(channelId, sender, message))
                .expectNextMatches(dto ->
                        dto.getMessage().equals(message) &&
                                dto.getProfileImage().equals(profileImage) &&
                                dto.getSenderName().equals(username))
                .verifyComplete();

        verify(chatRepository, times(1)).save(any(ChatMessage.class));
        verify(userRepository, times(1)).findById(sender);
    }

    @Test
    void createEnterMessage_Success() {
        ChatMessage enterMessage = ChatMessage.createEnterMessage(channelId, sender);
        when(chatRepository.save(any(ChatMessage.class)))
                .thenReturn(Mono.just(enterMessage));

        StepVerifier.create(chatService.createEnterMessage(channelId, sender))
                .expectNextMatches(dto ->
                        dto.getMessage().equals(username + "님이 입장하셨습니다.") &&
                                dto.getProfileImage().equals(profileImage) &&
                                dto.getSenderName().equals(username))
                .verifyComplete();

        verify(chatRepository, times(1)).save(any(ChatMessage.class));
        verify(userRepository, times(1)).findById(sender);
    }

    @Test
    void getChannelMessages_Success() {
        when(chatRepository.findByChannelIdOrderByTimestampAsc(channelId))
                .thenReturn(Flux.just(sampleChatMessage));

        StepVerifier.create(chatService.getChannelMessages(channelId))
                .expectNextMatches(dto ->
                        dto.getMessage().equals(message) &&
                                dto.getProfileImage().equals(profileImage) &&
                                dto.getSenderName().equals(username))
                .verifyComplete();

        verify(chatRepository, times(1))
                .findByChannelIdOrderByTimestampAsc(channelId);
        verify(userRepository, times(1)).findById(sender);
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