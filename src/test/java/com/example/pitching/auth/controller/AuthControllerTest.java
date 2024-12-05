package com.example.pitching.auth.controller;

import com.example.pitching.auth.dto.*;
import com.example.pitching.auth.service.AuthService;
import com.example.pitching.config.PermitAllConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@WebFluxTest(AuthController.class)
@Import(PermitAllConfig.class)
class AuthControllerTest {

    private static final String BASE_URL = "/api/v1/auth";
    private static final String TEST_EMAIL = "test@test.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_PROFILE_IMAGE = "profile.jpg";
    private static final String TEST_ACCESS_TOKEN = "accessToken123";
    private static final String TEST_REFRESH_TOKEN = "refreshToken123";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AuthService authService;

    private LoginRequest loginRequest;
    private LoginResponse loginResponse;
    private SignupRequest signupRequest;
    private RefreshRequest refreshRequest;
    private TokenInfo tokenInfo;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        tokenInfo = createTokenInfo(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);
        UserInfo userInfo = createUserInfo(TEST_EMAIL, TEST_USERNAME, TEST_PROFILE_IMAGE);
        loginRequest = createLoginRequest(TEST_EMAIL, TEST_PASSWORD);
        signupRequest = createSignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_USERNAME);
        refreshRequest = createRefreshRequest(TEST_REFRESH_TOKEN);
        loginResponse = createLoginResponse(tokenInfo, userInfo);
    }

    private TokenInfo createTokenInfo(String accessToken, String refreshToken) {
        return new TokenInfo(accessToken, refreshToken);
    }

    private UserInfo createUserInfo(String email, String username, String profileImage) {
        return new UserInfo(
                email,
                username,
                profileImage,
                Collections.emptyList()
        );
    }

    private LoginRequest createLoginRequest(String email, String password) {
        return new LoginRequest(email, password);
    }

    private SignupRequest createSignupRequest(String email, String password, String username) {
        return new SignupRequest(email, password, username);
    }

    private RefreshRequest createRefreshRequest(String refreshToken) {
        return new RefreshRequest(refreshToken);
    }

    private LoginResponse createLoginResponse(TokenInfo tokenInfo, UserInfo userInfo) {
        return new LoginResponse(tokenInfo, userInfo);
    }

    private WebTestClient.ResponseSpec performPost(String uri, Object body) {
        return webTestClient.post()
                .uri(BASE_URL + uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    private WebTestClient.ResponseSpec performGet(String uri, String email) {
        return webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + uri)
                        .queryParam("email", email)
                        .build())
                .exchange();
    }

    @Test
    @DisplayName("로그인 성공 시 200 OK와 함께 LoginResponse를 반환한다")
    void loginSuccess() {
        // given
        given(authService.authenticate(anyString(), anyString()))
                .willReturn(Mono.just(loginResponse));

        // when & then
        performPost("/login", loginRequest)
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .isEqualTo(loginResponse);
    }

    @Test
    @DisplayName("이메일 형식이 잘못된 경우 400 Bad Request를 반환한다")
    void loginWithInvalidEmail() {
        // given
        LoginRequest invalidRequest = createLoginRequest("invalid-email", TEST_PASSWORD);

        // when & then
        performPost("/login", invalidRequest)
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("토큰 갱신 성공 시 200 OK와 함께 TokenInfo를 반환한다")
    void refreshTokenSuccess() {
        // given
        given(authService.refreshToken(anyString()))
                .willReturn(Mono.just(tokenInfo));

        // when & then
        performPost("/refresh", refreshRequest)
                .expectStatus().isOk()
                .expectBody(TokenInfo.class)
                .isEqualTo(tokenInfo);
    }

    @Test
    @DisplayName("이메일 중복 확인 시 200 OK와 함께 ExistsEmailResponse를 반환한다")
    void checkEmailExists() {
        // given
        ExistsEmailResponse response = new ExistsEmailResponse(true);
        given(authService.existsEmail(anyString()))
                .willReturn(Mono.just(response));

        // when & then
        performGet("/check", TEST_EMAIL)
                .expectStatus().isOk()
                .expectBody(ExistsEmailResponse.class)
                .isEqualTo(response);
    }

    @Test
    @DisplayName("회원가입 성공 시 201 Created를 반환한다")
    void signupSuccess() {
        // given
        given(authService.signup(any(SignupRequest.class)))
                .willReturn(Mono.empty());

        // when & then
        performPost("/signup", signupRequest)
                .expectStatus().isCreated();
    }

    @Test
    @DisplayName("회원가입 시 비밀번호가 8자 미만이면 400 Bad Request를 반환한다")
    void signupWithInvalidPassword() {
        // given
        SignupRequest invalidRequest = createSignupRequest(
                TEST_EMAIL,
                "123",  // 8자 미만의 비밀번호
                TEST_USERNAME
        );

        // when & then
        performPost("/signup", invalidRequest)
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("회원가입 시 이메일 형식이 잘못된 경우 400 Bad Request를 반환한다")
    void signupWithInvalidEmail() {
        // given
        SignupRequest invalidRequest = createSignupRequest(
                "invalid-email",
                TEST_PASSWORD,
                TEST_USERNAME
        );

        // when & then
        performPost("/signup", invalidRequest)
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("회원가입 시 사용자 이름이 비어있으면 400 Bad Request를 반환한다")
    void signupWithEmptyUsername() {
        // given
        SignupRequest invalidRequest = createSignupRequest(
                TEST_EMAIL,
                TEST_PASSWORD,
                ""  // 빈 사용자 이름
        );

        // when & then
        performPost("/signup", invalidRequest)
                .expectStatus().isBadRequest();
    }
}