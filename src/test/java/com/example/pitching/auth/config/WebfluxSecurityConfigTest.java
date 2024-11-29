package com.example.pitching.auth.config;

import com.example.pitching.auth.jwt.JwtAuthenticationEntryPoint;
import com.example.pitching.auth.jwt.JwtTokenProvider;
import com.example.pitching.auth.oauth2.handler.OAuth2FailureHandler;
import com.example.pitching.auth.oauth2.handler.OAuth2SuccessHandler;
import com.example.pitching.auth.userdetails.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ActiveProfiles("test")
class WebfluxSecurityConfigTest {
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @Mock
    private OAuth2SuccessHandler oAuth2SuccessHandler;
    @Mock
    private OAuth2FailureHandler oAuth2FailureHandler;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private UserDetails userDetails;

    private WebfluxSecurityConfig webfluxSecurityConfig;
    private MockServerWebExchange corsExchange;
    private CorsConfiguration corsConfig;
    private AuthenticationWebFilter authenticationWebFilter;
    @Value("${front.url}")
    private String FRONTEND_URL;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String VALID_TOKEN = "valid.jwt.token";

    @BeforeEach
    void setUp() {
        webfluxSecurityConfig = new WebfluxSecurityConfig(
                jwtTokenProvider,
                jwtAuthenticationEntryPoint,
                oAuth2SuccessHandler,
                oAuth2FailureHandler,
                userDetailsService
        );
        ReflectionTestUtils.setField(webfluxSecurityConfig, "frontURL", FRONTEND_URL);

        // CORS 테스트를 위한 공통 설정
        MockServerHttpRequest corsRequest = MockServerHttpRequest.get("/").build();
        corsExchange = MockServerWebExchange.from(corsRequest);
        corsConfig = webfluxSecurityConfig.corsConfigurationSource()
                .getCorsConfiguration(corsExchange);

        // JWT 인증 필터 공통 설정
        Object filterObject = ReflectionTestUtils.invokeMethod(webfluxSecurityConfig, "jwtAuthenticationFilter");
        authenticationWebFilter = (AuthenticationWebFilter) filterObject;
    }

    @Test
    @DisplayName("CORS 설정이 프론트엔드 URL을 허용해야 한다")
    void corsConfigurationShouldAllowFrontendUrl() {
        assertThat(corsConfig.getAllowedOrigins()).contains(FRONTEND_URL);
    }

    @Test
    @DisplayName("CORS 설정이 필요한 HTTP 메서드를 허용해야 한다")
    void corsConfigurationShouldAllowRequiredHttpMethods() {
        assertThat(corsConfig.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    @Test
    @DisplayName("CORS 설정이 모든 헤더를 허용해야 한다")
    void corsConfigurationShouldAllowAllHeaders() {
        assertThat(corsConfig.getAllowedHeaders()).contains("*");
    }

    @Test
    @DisplayName("CORS 설정이 자격 증명을 허용해야 한다")
    void corsConfigurationShouldAllowCredentials() {
        assertThat(corsConfig.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("CORS 설정이 Location 헤더를 노출해야 한다")
    void corsConfigurationShouldExposeLocationHeader() {
        assertThat(corsConfig.getExposedHeaders()).contains("Location");
    }

    @Test
    @DisplayName("JWT 인증 필터가 올바른 토큰을 처리해야 한다")
    void jwtAuthenticationFilterShouldProcessValidToken() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtTokenProvider.validateAndGetEmail(VALID_TOKEN)).thenReturn(TEST_EMAIL);
        when(userDetailsService.findByUsername(TEST_EMAIL)).thenReturn(Mono.just(userDetails));

        // when
        Mono<MockServerWebExchange> result = authenticationWebFilter.filter(exchange, chain -> Mono.empty())
                .then(Mono.just(exchange));

        // then
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("공개 경로에서 인증을 건너뛰어야 한다")
    void shouldSkipAuthenticationForPublicPaths() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = authenticationWebFilter.filter(exchange, chain -> Mono.empty());

        // then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("잘못된 Authorization 헤더 형식에 대해 오류를 반환해야 한다")
    void shouldReturnErrorForInvalidAuthorizationHeader() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .header(HttpHeaders.AUTHORIZATION, "Invalid-Format")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = authenticationWebFilter.filter(exchange, chain -> Mono.empty());

        // then
        StepVerifier.create(result)
                .expectError(ResponseStatusException.class)
                .verify();
    }
}