package com.example.pitching.auth.service;

import com.example.pitching.auth.domain.TokenStatus;
import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.dto.TokenInfo;
import com.example.pitching.auth.dto.SignupRequest;
import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.auth.jwt.JwtTokenProvider;
import com.example.pitching.user.domain.Channel;
import com.example.pitching.user.domain.Server;
import com.example.pitching.user.domain.utility.ChannelCategory;
import com.example.pitching.user.repository.ChannelRepository;
import com.example.pitching.user.repository.ServerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private ServerRepository serverRepository;
    @Mock
    private ChannelRepository channelRepository;

    @InjectMocks
    private AuthService authService;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password";
    private final String TEST_USERNAME = "testUser";
    private final String ACCESS_TOKEN = "accessToken";
    private final String REFRESH_TOKEN = "refreshToken";

    private TokenInfo tokenInfo;

    @BeforeEach
    void setUp() {
        String ENCODED_PASSWORD = "encodedPassword";
        User testUser = User.createNewUser(TEST_EMAIL, TEST_USERNAME, null, ENCODED_PASSWORD);
        Server testServer = Server.createNewServer("Test Server", null);
        Channel testChannel = Channel.createNewChannel(1L, "Test Channel", ChannelCategory.CHAT, 1);
        tokenInfo = new TokenInfo(ACCESS_TOKEN, REFRESH_TOKEN);

        lenient().when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        lenient().when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Mono.just(testUser));
        lenient().when(serverRepository.findServersByUserEmail(TEST_EMAIL)).thenReturn(Flux.just(testServer));
        lenient().when(channelRepository.findByServerId(any())).thenReturn(Flux.just(testChannel));
        lenient().when(jwtTokenProvider.createTokenInfo(TEST_EMAIL)).thenReturn(tokenInfo);
    }

    @Test
    @DisplayName("올바른 이메일과 비밀번호로 로그인 시 LoginResponse를 반환한다")
    void authenticate_WithValidCredentials_ReturnsLoginResponse() {
        // when & then
        StepVerifier.create(authService.authenticate(TEST_EMAIL, TEST_PASSWORD))
                .assertNext(response ->
                        assertThat(response.tokenInfo().accessToken()).isEqualTo(ACCESS_TOKEN)
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("잘못된 이메일로 로그인 시 Unauthorized 에러를 반환한다")
    void authenticate_WithInvalidEmail_ThrowsUnauthorized() {
        // given
        when(userRepository.findByEmail("wrong@email.com")).thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(authService.authenticate("wrong@email.com", TEST_PASSWORD))
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isInstanceOf(ResponseStatusException.class)
                                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED)
                )
                .verify();
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 새로운 액세스 토큰을 발급받는다")
    void refreshToken_WithValidToken_ReturnsNewTokenInfo() {
        // given
        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN)).thenReturn(TokenStatus.VALID);
        when(jwtTokenProvider.extractEmail(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(jwtTokenProvider.recreateAccessToken(TEST_EMAIL)).thenReturn(tokenInfo);

        // when & then
        StepVerifier.create(authService.refreshToken(REFRESH_TOKEN))
                .assertNext(response ->
                        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN)
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("새로운 이메일로 회원가입을 시도하면 성공한다")
    void signup_WithNewEmail_Succeeds() {
        // given
        SignupRequest request = new SignupRequest(TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD);
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(Mono.just(false));
        when(userRepository.insertUser(any(), any(), any(), any())).thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(authService.signup(request))
                .verifyComplete();
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입 시 Conflict 에러를 반환한다")
    void signup_WithExistingEmail_ThrowsConflict() {
        // given
        SignupRequest request = new SignupRequest(TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD);
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(Mono.just(true));

        // when & then
        StepVerifier.create(authService.signup(request))
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isInstanceOf(ResponseStatusException.class)
                                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT)
                )
                .verify();
    }
}