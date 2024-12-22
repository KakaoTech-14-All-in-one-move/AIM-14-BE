package com.example.pitching.chat.service;

import com.example.pitching.chat.config.RabbitMQConfig;
import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.dto.ChatMessageDTO;
import com.example.pitching.chat.repository.ChatRepository;
import com.example.pitching.chat.repository.ChatRedisRepository;
import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatRedisRepository chatRedisRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ChatService chatService;

    private ChatMessage sampleChatMessage;
    private User sampleUser;
    private ChatMessageDTO sampleMessageDTO;
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
        sampleMessageDTO = ChatMessageDTO.from(sampleChatMessage, sampleUser);
    }

    @Test
    void saveTalkMessage_Success() {
        when(userRepository.findById(sender))
                .thenReturn(Mono.just(sampleUser));
        doNothing().when(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.CHAT_EXCHANGE),
                eq(RabbitMQConfig.CHAT_ROUTING_KEY),
                any(ChatMessageDTO.class)
        );

        StepVerifier.create(chatService.saveTalkMessage(channelId, sender, message))
                .expectNextMatches(dto -> {
                    boolean matches = dto.getMessage().equals(message) &&
                            dto.getProfile_image().equals(profileImage) &&
                            dto.getSenderName().equals(username);
                    if (matches) {
                        verify(userRepository, times(1)).findById(sender);
                        verify(rabbitTemplate, times(1)).convertAndSend(
                                eq(RabbitMQConfig.CHAT_EXCHANGE),
                                eq(RabbitMQConfig.CHAT_ROUTING_KEY),
                                any(ChatMessageDTO.class)
                        );
                    }
                    return matches;
                })
                .verifyComplete();
    }

    @Test
    void saveMessageToDynamoDB_Success() {
        when(chatRepository.save(any(ChatMessage.class)))
                .thenReturn(Mono.just(sampleChatMessage));

        StepVerifier.create(chatService.saveMessageToDynamoDB(sampleMessageDTO))
                .expectNextMatches(dto -> {
                    boolean matches = dto.getMessage().equals(message) &&
                            dto.getChannelId().equals(channelId);
                    if (matches) {
                        verify(chatRepository, times(1)).save(any(ChatMessage.class));
                    }
                    return matches;
                })
                .verifyComplete();
    }

    @Test
    void getChannelMessages_Success() {
        when(chatRedisRepository.getRecentMessages(channelId))
                .thenReturn(Flux.empty());
        when(chatRepository.findByChannelIdOrderByTimestampAsc(channelId))
                .thenReturn(Flux.just(sampleChatMessage));
        when(userRepository.findById(sender))
                .thenReturn(Mono.just(sampleUser));
        when(chatRedisRepository.saveMessage(any(ChatMessageDTO.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(chatService.getChannelMessages(channelId))
                .expectNextMatches(dto -> {
                    boolean matches = dto.getMessage().equals(message) &&
                            dto.getProfile_image().equals(profileImage) &&
                            dto.getSenderName().equals(username);
                    if (matches) {
                        verify(chatRepository, times(1))
                                .findByChannelIdOrderByTimestampAsc(channelId);
                        verify(userRepository, times(1)).findById(sender);
                    }
                    return matches;
                })
                .verifyComplete();
    }

    @Test
    void getChannelMessages_WithNullChannelId() {
        when(chatRepository.findByChannelIdOrderByTimestampAsc(null))
                .thenReturn(Flux.empty());

        when(chatRedisRepository.getRecentMessages(null))
                .thenReturn(Flux.empty());

        StepVerifier.create(chatService.getChannelMessages(null))
                .verifyComplete();
    }

    @Test
    void deleteChannelMessages_Success() {
        when(chatRepository.deleteByChannelId(channelId))
                .thenReturn(Mono.empty());
        when(chatRedisRepository.deleteChannelMessages(channelId))
                .thenReturn(Mono.empty());

        StepVerifier.create(chatService.deleteChannelMessages(channelId))
                .verifyComplete();

        verify(chatRepository, times(1)).deleteByChannelId(channelId);
        verify(chatRedisRepository, times(1)).deleteChannelMessages(channelId);
    }

    @Test
    void deleteChannelMessages_Error() {
        RuntimeException expectedException = new RuntimeException("Delete Error");
        when(chatRepository.deleteByChannelId(channelId))
                .thenReturn(Mono.error(expectedException));
        when(chatRedisRepository.deleteChannelMessages(channelId))
                .thenReturn(Mono.empty());

        StepVerifier.create(chatService.deleteChannelMessages(channelId))
                .expectError(RuntimeException.class)
                .verify();

        verify(chatRepository, times(1)).deleteByChannelId(channelId);
        verify(chatRedisRepository, times(1)).deleteChannelMessages(channelId);
    }
}