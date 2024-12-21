package com.example.pitching.chat.handler;

import com.example.pitching.auth.domain.User;
import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.dto.ChatMessageDTO;
import com.example.pitching.chat.dto.UserUpdateMessage;
import com.example.pitching.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {

    private ChatWebSocketHandler chatWebSocketHandler;

    @Mock
    private ChatService chatService;

    @Mock
    private WebSocketSession session;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Long CHANNEL_ID = 1L;
    private final String SESSION_ID = "test-session-id";
    private final String SENDER_EMAIL = "test@example.com";
    private final String SENDER_NAME = "Test User";
    private final String MESSAGE = "Hello, World!";
    private final String PROFILE_IMAGE = "profile.jpg";
    private List<String> sentMessages;
    private User testUser;

    @BeforeEach
    void setUp() {
        chatWebSocketHandler = new ChatWebSocketHandler(objectMapper, chatService);
        sentMessages = new ArrayList<>();
        testUser = User.createNewUser(SENDER_EMAIL, SENDER_NAME, PROFILE_IMAGE, "password");

        // 기본 WebSocketSession 모의 설정 - lenient 사용
        lenient().when(session.getId()).thenReturn(SESSION_ID);
        lenient().when(session.close()).thenReturn(Mono.empty());
        lenient().when(session.send(any())).thenAnswer(invocation -> {
            Mono<WebSocketMessage> msgMono = invocation.getArgument(0);
            msgMono.subscribe(msg -> sentMessages.add(msg.getPayloadAsText()));
            return Mono.empty();
        });
        lenient().when(session.textMessage(anyString())).thenAnswer(invocation -> {
            String payload = invocation.getArgument(0);
            WebSocketMessage message = mock(WebSocketMessage.class);
            lenient().when(message.getPayloadAsText()).thenReturn(payload);
            return message;
        });

        // 기본적으로 연결 유지
        lenient().when(session.receive())
                .thenReturn(Flux.never());
    }

    @Test
    @DisplayName("구독 메시지 처리 성공")
    void handleSubscribe_Success() {
        // given
        WebSocketCommand subscribeCommand = new WebSocketCommand();
        subscribeCommand.setType("SUBSCRIBE");
        subscribeCommand.setChannelId(CHANNEL_ID);
        subscribeCommand.setDestination("/ws/chat/" + CHANNEL_ID);
        String subscribePayload = writeValueAsString(subscribeCommand);

        WebSocketMessage subscribeMessage = mock(WebSocketMessage.class);
        when(subscribeMessage.getPayloadAsText()).thenReturn(subscribePayload);

        // when
        when(session.receive()).thenReturn(
                Flux.just(subscribeMessage)
                        .concatWith(Flux.never())
        );

        // then
        StepVerifier.create(chatWebSocketHandler.handle(session))
                .expectSubscription()
                .then(() -> {
                    verify(session, atLeastOnce()).getId();
                    assertThat(sentMessages).isEmpty();
                })
                .thenCancel()
                .verify();
    }

    @Test
    @DisplayName("채팅 메시지 전송 성공")
    void handleSend_Success() {
        // given
        WebSocketCommand.ChatMessagePayload messagePayload = new WebSocketCommand.ChatMessagePayload();
        messagePayload.setChannelId(CHANNEL_ID);
        messagePayload.setMessage(MESSAGE);
        messagePayload.setId(SENDER_EMAIL);
        messagePayload.setType("TALK");

        WebSocketCommand sendCommand = new WebSocketCommand();
        sendCommand.setType("SEND");
        sendCommand.setChannelId(CHANNEL_ID);
        sendCommand.setDestination("/ws/chat/" + CHANNEL_ID);
        sendCommand.setPayload(messagePayload);

        ChatMessage chatMessage = ChatMessage.createTalkMessage(CHANNEL_ID, SENDER_EMAIL, MESSAGE);
        ChatMessageDTO responseDto = ChatMessageDTO.from(chatMessage, testUser);

        when(chatService.saveTalkMessage(CHANNEL_ID, SENDER_EMAIL, MESSAGE))
                .thenReturn(Mono.just(responseDto));

        WebSocketMessage subscribeMessage = mock(WebSocketMessage.class);
        when(subscribeMessage.getPayloadAsText()).thenReturn(writeValueAsString(createSubscribeCommand(CHANNEL_ID)));

        WebSocketMessage sendMessage = mock(WebSocketMessage.class);
        when(sendMessage.getPayloadAsText()).thenReturn(writeValueAsString(sendCommand));

        // when
        when(session.receive()).thenReturn(
                Flux.just(subscribeMessage, sendMessage)
                        .concatWith(Flux.never())
        );

        // then
        StepVerifier.create(chatWebSocketHandler.handle(session))
                .expectSubscription()
                .then(() -> {
                    verify(session, atLeastOnce()).send(any());
                    assertThat(sentMessages).isNotEmpty();
                    String sentMessage = sentMessages.get(sentMessages.size() - 1);
                    assertThat(sentMessage).contains(MESSAGE);
                    assertThat(sentMessage).contains(SENDER_EMAIL);
                })
                .thenCancel()
                .verify();
    }

    @Test
    @DisplayName("사용자 정보 업데이트 브로드캐스트 성공")
    void broadcastUserUpdate_Success() {
        // given
        WebSocketCommand subscribeCommand = createSubscribeCommand(CHANNEL_ID);
        WebSocketMessage subscribeMessage = mock(WebSocketMessage.class);
        when(subscribeMessage.getPayloadAsText()).thenReturn(writeValueAsString(subscribeCommand));
        when(session.receive()).thenReturn(
                Flux.just(subscribeMessage)
                        .concatWith(Flux.never())
        );

        chatWebSocketHandler.handle(session).subscribe();
        sentMessages.clear();

        UserUpdateMessage updateMessage = new UserUpdateMessage(
                SENDER_EMAIL,
                SENDER_NAME,
                PROFILE_IMAGE
        );

        // when & then
        StepVerifier.create(chatWebSocketHandler.broadcastUserUpdate(updateMessage))
                .expectSubscription()
                .then(() -> {
                    verify(session, atLeastOnce()).send(any());
                    assertThat(sentMessages).isNotEmpty();
                    String sentMessage = sentMessages.get(sentMessages.size() - 1);
                    assertThat(sentMessage).contains(SENDER_EMAIL);
                    assertThat(sentMessage).contains(SENDER_NAME);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("채널 연결 종료 성공")
    void closeChannelConnections_Success() {
        // given
        WebSocketCommand subscribeCommand = createSubscribeCommand(CHANNEL_ID);
        WebSocketMessage subscribeMessage = mock(WebSocketMessage.class);
        when(subscribeMessage.getPayloadAsText()).thenReturn(writeValueAsString(subscribeCommand));
        when(session.receive()).thenReturn(
                Flux.just(subscribeMessage)
                        .concatWith(Flux.never())
        );
        chatWebSocketHandler.handle(session).subscribe();

        // when & then
        StepVerifier.create(chatWebSocketHandler.closeChannelConnections(CHANNEL_ID))
                .expectSubscription()
                .then(() -> verify(session, atLeastOnce()).close())
                .verifyComplete();
    }

    private WebSocketCommand createSubscribeCommand(Long channelId) {
        WebSocketCommand command = new WebSocketCommand();
        command.setType("SUBSCRIBE");
        command.setChannelId(channelId);
        command.setDestination("/ws/chat/" + channelId);
        return command;
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}