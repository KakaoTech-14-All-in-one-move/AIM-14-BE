package com.example.pitching.auth.oauth2.handler;

import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.dto.TokenInfo;
import com.example.pitching.auth.jwt.JwtTokenProvider;
import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.user.domain.Channel;
import com.example.pitching.user.domain.Server;
import com.example.pitching.user.repository.ChannelRepository;
import com.example.pitching.user.repository.ServerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class OAuth2HandlerTest {

    @Nested
    @DisplayName("OAuth2 성공 핸들러 테스트")
    class SuccessHandlerTest {
        @Mock private JwtTokenProvider jwtTokenProvider;
        @Mock private UserRepository userRepository;
        @Mock private ServerRepository serverRepository;
        @Mock private ChannelRepository channelRepository;

        private final String TEST_EMAIL = "test@example.com";
        private final String TEST_NAME = "TestUser";
        private final Long TEST_USER_ID = 1L;
        @Value("${front.url}")
        private String FRONT_URL;
        private MockServerWebExchange exchange;
        private Mono<Void> result;

        @BeforeEach
        void setUp() {
            // 핸들러 및 기본 설정 준비
            OAuth2SuccessHandler successHandler = new OAuth2SuccessHandler(
                    jwtTokenProvider,
                    userRepository,
                    serverRepository,
                    channelRepository
            );
            ReflectionTestUtils.setField(successHandler, "frontURL", FRONT_URL);

            MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
            exchange = MockServerWebExchange.from(request);
            WebFilterExchange webFilterExchange = new WebFilterExchange(exchange, chain -> Mono.empty());

            // OAuth2 인증 정보 준비
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("email", TEST_EMAIL);
            attributes.put("name", TEST_NAME);

            OAuth2User oauth2User = new DefaultOAuth2User(
                    Collections.emptyList(),
                    attributes,
                    "email"
            );

            OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                    oauth2User,
                    Collections.emptyList(),
                    "google"
            );

            // 모의 응답 설정
            User user = User.createNewUser("google@" + TEST_EMAIL, TEST_NAME, null, null, TEST_USER_ID);
            TokenInfo tokenInfo = new TokenInfo("accessToken", "refreshToken");
            Server server = Server.createNewServer("serverName", "serverImage");
            ReflectionTestUtils.setField(server, "serverId", 1L);
            Channel channel = Channel.createNewChannel(1L, "general", "TEXT", 0);

            when(userRepository.findByEmail("google@" + TEST_EMAIL)).thenReturn(Mono.just(user));
            when(jwtTokenProvider.createTokenInfo("google@" + TEST_EMAIL, TEST_USER_ID)).thenReturn(tokenInfo);
            when(serverRepository.findServersByUserEmail("google@" + TEST_EMAIL)).thenReturn(Flux.just(server));
            when(channelRepository.findByServerId(1L)).thenReturn(Flux.just(channel));

            // 실행
            result = successHandler.onAuthenticationSuccess(webFilterExchange, authentication);
        }

        @Test
        @DisplayName("OAuth2 로그인 성공 시 Mono가 완료되어야 한다")
        void shouldCompleteMonoOnSuccess() {
            StepVerifier.create(result)
                    .expectComplete()
                    .verify();
        }

        @Test
        @DisplayName("OAuth2 로그인 성공 시 임시 리다이렉트 상태여야 한다")
        void shouldHaveTemporaryRedirectStatus() {
            StepVerifier.create(result)
                    .expectComplete()
                    .verify();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.TEMPORARY_REDIRECT);
        }

        @Test
        @DisplayName("OAuth2 로그인 성공 시 올바른 콜백 URL로 리다이렉트되어야 한다")
        void shouldRedirectToCorrectCallbackUrl() {
            StepVerifier.create(result)
                    .expectComplete()
                    .verify();

            assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION))
                    .startsWith(FRONT_URL + "oauth2/callback");
        }

        @Test
        @DisplayName("리다이렉트 URL에 올바른 토큰 정보가 포함되어야 한다")
        void shouldIncludeCorrectTokensInRedirectUrl() {
            StepVerifier.create(result)
                    .expectComplete()
                    .verify();

            String location = exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION);
            assertThat(URI.create(location).getQuery())
                    .contains("accessToken=accessToken")
                    .contains("refreshToken=refreshToken");
        }

        @Test
        @DisplayName("리다이렉트 URL에 올바른 사용자 정보가 포함되어야 한다")
        void shouldIncludeCorrectUserInfoInRedirectUrl() {
            StepVerifier.create(result)
                    .expectComplete()
                    .verify();

            String location = exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION);
            assertThat(URI.create(location).getQuery())
                    .contains("email=google@" + TEST_EMAIL)
                    .contains("username=" + TEST_NAME)
                    .contains("user_id=" + TEST_USER_ID);  // userId 검증 추가
        }

        @Test
        @DisplayName("리다이렉트 URL에 서버 정보가 포함되어야 한다")
        void shouldIncludeServerInfoInRedirectUrl() {
            StepVerifier.create(result)
                    .expectComplete()
                    .verify();

            String location = exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION);
            assertThat(URI.create(location).getQuery())
                    .contains("servers=");
        }
    }

    @Nested
    @DisplayName("OAuth2 실패 핸들러 테스트")
    class FailureHandlerTest {
        private static final String TEST_FRONT_URL = "http://localhost:5173/";
        private OAuth2FailureHandler failureHandler;
        private WebFilterExchange webFilterExchange;

        @BeforeEach
        void setUp() {
            failureHandler = new OAuth2FailureHandler();
            ReflectionTestUtils.setField(failureHandler, "frontURL", TEST_FRONT_URL);
            MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            webFilterExchange = new WebFilterExchange(exchange, chain -> Mono.empty());
        }

        @Test
        @DisplayName("인증 실패 시 프론트엔드로 리다이렉션되어야 한다")
        void shouldRedirectToFrontendOnFailure() {
            // when
            Mono<Void> result = failureHandler.onAuthenticationFailure(
                    webFilterExchange,
                    new BadCredentialsException("Authentication failed")
            );

            // then
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(webFilterExchange.getExchange().getResponse().getHeaders().getLocation())
                    .isEqualTo(URI.create(TEST_FRONT_URL));
        }
    }
}