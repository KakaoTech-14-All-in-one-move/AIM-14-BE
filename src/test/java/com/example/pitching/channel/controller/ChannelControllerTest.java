package com.example.pitching.channel.controller;

import com.example.pitching.auth.jwt.JwtTokenProvider;
import com.example.pitching.auth.userdetails.CustomUserDetailsService;
import com.example.pitching.config.SecurityTestConfig;
import com.example.pitching.user.controller.ChannelController;
import com.example.pitching.user.domain.Channel;
import com.example.pitching.user.domain.utility.ChannelCategory;
import com.example.pitching.user.dto.ChannelResponse;
import com.example.pitching.user.dto.CreateChannelRequest;
import com.example.pitching.user.dto.UpdateChannelNameRequest;
import com.example.pitching.user.service.ChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.when;

@WebFluxTest(ChannelController.class)
@Import(SecurityTestConfig.class)
class ChannelControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ChannelService channelService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private static final String BASE_URL = "/api/v1/servers/{server_id}/channels";
    private static final Long SERVER_ID = 1L;
    private static final Long CHANNEL_ID = 1L;
    private static final String VALID_TOKEN = "valid-test-token";
    private static final String DEFAULT_CHANNEL_NAME = "general";
    private static final String UPDATED_CHANNEL_NAME = "updated-channel";
    private static final String TEST_EMAIL = "test@example.com";

    private Channel testChannel;
    private CreateChannelRequest createChannelRequest;

    @BeforeEach
    void setUp() {
        testChannel = createTestChannel(DEFAULT_CHANNEL_NAME, ChannelCategory.CHAT);
        createChannelRequest = createChannelRequest(DEFAULT_CHANNEL_NAME, ChannelCategory.CHAT);
        setupAuthentication();
    }

    private void setupAuthentication() {
        when(jwtTokenProvider.validateAndGetEmail(VALID_TOKEN))
                .thenReturn(TEST_EMAIL);

        UserDetails userDetails = User.builder()
                .username(TEST_EMAIL)
                .password("password")
                .roles("USER")
                .build();
        when(userDetailsService.findByUsername(TEST_EMAIL))
                .thenReturn(Mono.just(userDetails));
    }

    private Channel createTestChannel(String name, String category) {
        return Channel.createNewChannel(SERVER_ID, name, category, 0);
    }

    private CreateChannelRequest createChannelRequest(String name, String category) {
        return new CreateChannelRequest(name, category);
    }

    private WebTestClient.RequestHeadersSpec<?> authenticatedRequest(
            WebTestClient.RequestHeadersSpec<?> request) {
        return request.headers(headers -> headers.setBearerAuth(VALID_TOKEN));
    }

    private WebTestClient.ResponseSpec performPost(String uri, Object body) {
        return authenticatedRequest(webTestClient.post()
                .uri(uri, SERVER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body))
                .exchange();
    }

    private WebTestClient.ResponseSpec performGet(String uri) {
        return authenticatedRequest(webTestClient.get()
                .uri(uri, SERVER_ID))
                .exchange();
    }

    private WebTestClient.ResponseSpec performPut(String uri, Object body) {
        return authenticatedRequest(webTestClient.put()
                .uri(uri, SERVER_ID, CHANNEL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body))
                .exchange();
    }

    private WebTestClient.ResponseSpec performDelete(String uri) {
        return authenticatedRequest(webTestClient.delete()
                .uri(uri, SERVER_ID, CHANNEL_ID))
                .exchange();
    }

    @Test
    @DisplayName("채널 생성 성공")
    void createChannel_Success() {
        when(channelService.createChannel(SERVER_ID, createChannelRequest))
                .thenReturn(Mono.just(testChannel));

        performPost(BASE_URL, createChannelRequest)
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.channelName").isEqualTo(DEFAULT_CHANNEL_NAME)
                .jsonPath("$.channelCategory").isEqualTo(ChannelCategory.CHAT.toString());
    }

    @Test
    @DisplayName("채널 생성 실패 - 인증되지 않은 사용자")
    void createChannel_Failure_Unauthorized() {
        webTestClient.post()
                .uri(BASE_URL, SERVER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createChannelRequest)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("채널 생성 실패 - 동일 카테고리 내 중복된 채널명")
    void createChannel_Failure_DuplicateName() {
        String errorMessage = "동일한 카테고리 내에 같은 이름의 채널이 이미 존재합니다.";
        when(channelService.createChannel(SERVER_ID, createChannelRequest))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, errorMessage)));

        performPost(BASE_URL, createChannelRequest)
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.message").isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("채널 생성 성공 - 다른 카테고리의 동일 채널명")
    void createChannel_Success_SameNameDifferentCategory() {
        CreateChannelRequest voiceChannelRequest = createChannelRequest(DEFAULT_CHANNEL_NAME, ChannelCategory.VOICE);
        Channel voiceChannel = createTestChannel(DEFAULT_CHANNEL_NAME, ChannelCategory.VOICE);

        when(channelService.createChannel(SERVER_ID, voiceChannelRequest))
                .thenReturn(Mono.just(voiceChannel));

        performPost(BASE_URL, voiceChannelRequest)
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.channelName").isEqualTo(DEFAULT_CHANNEL_NAME)
                .jsonPath("$.channelCategory").isEqualTo(ChannelCategory.VOICE.toString());
    }

    @Test
    @DisplayName("채널명 수정 성공")
    void updateChannelName_Success() {
        UpdateChannelNameRequest request = new UpdateChannelNameRequest(UPDATED_CHANNEL_NAME);
        Channel updatedChannel = createTestChannel(UPDATED_CHANNEL_NAME, ChannelCategory.CHAT);

        when(channelService.updateChannelName(CHANNEL_ID, UPDATED_CHANNEL_NAME))
                .thenReturn(Mono.just(updatedChannel));

        performPut(BASE_URL + "/{channel_id}/name", request)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.channelName").isEqualTo(UPDATED_CHANNEL_NAME);
    }

    @Test
    @DisplayName("채널 삭제 성공")
    void deleteChannel_Success() {
        when(channelService.deleteChannel(CHANNEL_ID))
                .thenReturn(Mono.empty());

        performDelete(BASE_URL + "/{channel_id}")
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("서버의 모든 채널 조회 성공")
    void getServerChannels_Success() {
        List<Channel> channels = List.of(
                createTestChannel("channel-1", ChannelCategory.CHAT),
                createTestChannel("channel-2", ChannelCategory.CHAT)
        );

        when(channelService.getChannelsByServerId(SERVER_ID))
                .thenReturn(Flux.fromIterable(channels));

        performGet(BASE_URL)
                .expectStatus().isOk()
                .expectBodyList(ChannelResponse.class)
                .hasSize(2)
                .contains(
                        ChannelResponse.from(channels.get(0)),
                        ChannelResponse.from(channels.get(1)));
    }

    @Test
    @DisplayName("채널 조회 실패 - 존재하지 않는 서버")
    void getServerChannels_Failure_InvalidServer() {
        when(channelService.getChannelsByServerId(SERVER_ID))
                .thenReturn(Flux.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다.")));

        performGet(BASE_URL)
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("잘못된 토큰으로 채널 조회 실패")
    void getServerChannels_Failure_InvalidToken() {
        String invalidToken = "invalid-token";
        when(jwtTokenProvider.validateAndGetEmail(invalidToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        webTestClient.get()
                .uri(BASE_URL, SERVER_ID)
                .headers(headers -> headers.setBearerAuth(invalidToken))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}