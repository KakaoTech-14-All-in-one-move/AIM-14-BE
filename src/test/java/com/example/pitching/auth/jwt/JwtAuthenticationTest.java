package com.example.pitching.auth.jwt;

import com.example.pitching.auth.userdetails.CustomUserDetailsService;
import com.example.pitching.config.SecurityTestConfig;
import com.example.pitching.user.controller.ChannelController;
import com.example.pitching.user.service.ChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@WebFluxTest(ChannelController.class)
@Import(SecurityTestConfig.class)
class JwtAuthenticationTest {
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
    private static final String VALID_TOKEN = "valid.test.token";
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        // JWT 토큰 검증 모의 설정
        when(jwtTokenProvider.validateAndGetEmail(VALID_TOKEN))
                .thenReturn(TEST_EMAIL);

        // UserDetails 모의 설정
        UserDetails userDetails = User.withUsername(TEST_EMAIL)
                .password("password")
                .roles("USER")
                .build();
        when(userDetailsService.findByUsername(TEST_EMAIL))
                .thenReturn(Mono.just(userDetails));
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 인증 성공")
    void authenticateWithValidToken() {
        when(channelService.getChannelsByServerId(SERVER_ID))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri(BASE_URL, SERVER_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("JWT 토큰 없이 접근 시 인증 실패")
    void failsWithoutToken() {
        webTestClient.get()
                .uri(BASE_URL, SERVER_ID)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("잘못된 형식의 JWT 토큰으로 인증 실패")
    void failsWithInvalidToken() {
        String invalidToken = "invalid.token";
        when(jwtTokenProvider.validateAndGetEmail(invalidToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        webTestClient.get()
                .uri(BASE_URL, SERVER_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("만료된 JWT 토큰으로 인증 실패")
    void failsWithExpiredToken() {
        String expiredToken = "expired.token";
        when(jwtTokenProvider.validateAndGetEmail(expiredToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has expired"));

        webTestClient.get()
                .uri(BASE_URL, SERVER_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}