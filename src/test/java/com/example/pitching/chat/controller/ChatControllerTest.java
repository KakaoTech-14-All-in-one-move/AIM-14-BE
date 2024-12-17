package com.example.pitching.chat.controller;

import com.example.pitching.auth.jwt.JwtAuthenticationEntryPoint;
import com.example.pitching.auth.jwt.JwtTokenProvider;
import com.example.pitching.auth.oauth2.handler.OAuth2FailureHandler;
import com.example.pitching.auth.oauth2.handler.OAuth2SuccessHandler;
import com.example.pitching.auth.userdetails.CustomUserDetailsService;
import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.service.ChatService;
import com.example.pitching.config.PermitAllConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@WebFluxTest(ChatController.class)
@Import(PermitAllConfig.class)
class ChatControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ChatService chatService;

    private static final String BASE_URL = "/ws/v1/channels/{channel_id}/messages";
    private static final Long CHANNEL_ID = 1L;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "Test User";
    private static final String TEST_MESSAGE = "Test message";
    private static final String TEST_PROFILE_IMAGE = "test-profile-image.jpg";

    private ChatMessage testMessage;
    private List<ChatMessage> testMessages;

    @BeforeEach
    void setUp() {
        // Given
        testMessage = ChatMessage.createTalkMessage(
                CHANNEL_ID,
                TEST_EMAIL,
                TEST_USERNAME,
                TEST_MESSAGE,
                TEST_PROFILE_IMAGE
        );
        testMessages = List.of(testMessage);

        // Default responses
        when(chatService.getChannelMessages(any()))
                .thenReturn(Flux.empty());
        when(chatService.deleteChannelMessages(any()))
                .thenReturn(Mono.empty());

        // Success case setup
        when(chatService.getChannelMessages(CHANNEL_ID))
                .thenReturn(Flux.fromIterable(testMessages));

        // Error case setup
        when(chatService.deleteChannelMessages(CHANNEL_ID))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 채널입니다.")));
    }

    @Test
    @DisplayName("채널 메시지 조회 성공 - 메시지 수 확인")
    void getChannelMessages_Success_CheckSize() {
        webTestClient.get()
                .uri(BASE_URL, CHANNEL_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ChatMessage.class)
                .value(messageList -> assertThat(messageList).hasSize(1));
    }

    @Test
    @DisplayName("채널 메시지 조회 성공 - 메시지 내용 확인")
    void getChannelMessages_Success_CheckContent() {
        webTestClient.get()
                .uri(BASE_URL, CHANNEL_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ChatMessage.class)
                .value(messageList ->
                        assertThat(messageList.get(0).getMessage()).isEqualTo(TEST_MESSAGE)
                );
    }

    @Test
    @DisplayName("채널 메시지 조회 성공 - 발신자 확인")
    void getChannelMessages_Success_CheckSender() {
        webTestClient.get()
                .uri(BASE_URL, CHANNEL_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ChatMessage.class)
                .value(messageList ->
                        assertThat(messageList.get(0).getSender()).isEqualTo(TEST_EMAIL)
                );
    }

    @Test
    @DisplayName("채널 메시지 조회 실패 - 존재하지 않는 채널")
    void getChannelMessages_Failure_InvalidChannel() {
        when(chatService.getChannelMessages(CHANNEL_ID))
                .thenReturn(Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 채널입니다.")));

        webTestClient.get()
                .uri(BASE_URL, CHANNEL_ID)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("타임스탬프 이후의 메시지 조회 성공")
    void getChannelMessages_WithTimestamp_Success() {
        Long timestamp = System.currentTimeMillis();

        webTestClient.get()
                .uri(BASE_URL + "?timestamp=" + timestamp, CHANNEL_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ChatMessage.class)
                .value(messageList ->
                        assertThat(messageList.get(0).getChannelId()).isEqualTo(CHANNEL_ID)
                );
    }

    @Test
    @DisplayName("채널 메시지 삭제 성공")
    void deleteChannelMessages_Success() {
        when(chatService.deleteChannelMessages(CHANNEL_ID))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri(BASE_URL, CHANNEL_ID)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("채널 메시지 삭제 실패 - 존재하지 않는 채널")
    void deleteChannelMessages_Failure_InvalidChannel() {
        webTestClient.delete()
                .uri(BASE_URL, CHANNEL_ID)
                .exchange()
                .expectStatus().isBadRequest();
    }
}