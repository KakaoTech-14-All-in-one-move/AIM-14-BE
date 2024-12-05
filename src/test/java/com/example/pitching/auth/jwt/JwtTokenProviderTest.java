package com.example.pitching.auth.jwt;

import com.example.pitching.auth.domain.TokenStatus;
import com.example.pitching.auth.dto.TokenInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // JwtTokenProvider 인스턴스 생성
        jwtTokenProvider = new JwtTokenProvider();

        // 필드값 설정
        ReflectionTestUtils.setField(jwtTokenProvider, "secret",
                "c3ByaW5nLWJvb3Qtc2VjdXJpdHktand0LXR1dG9yaWFsLWppd29vbi1zcHJpbmctYm9vdC1zZWN1cml0eS1qd3QtdHV0b3JpYWwK");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", Duration.ofMinutes(30));
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", Duration.ofDays(14));
    }

    @Test
    @DisplayName("액세스 토큰 생성 성공")
    void createAccessToken() {
        TokenInfo tokenInfo = jwtTokenProvider.createTokenInfo("test@example.com");

        assertNotNull(tokenInfo.accessToken());
        assertNotNull(tokenInfo.refreshToken());
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void validateValidToken() {
        String token = jwtTokenProvider.createTokenInfo("test@example.com").accessToken();
        String email = jwtTokenProvider.validateAndGetEmail(token);

        assertEquals("test@example.com", email);
    }

    @Test
    @DisplayName("만료된 토큰 검증 실패")
    void validateExpiredToken() {
        // accessTokenExpiration을 임시로 0으로 설정하여 만료된 토큰 생성
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", Duration.ZERO);
        String token = jwtTokenProvider.createTokenInfo("test@example.com").accessToken();

        assertThrows(ResponseStatusException.class, () -> {
            jwtTokenProvider.validateAndGetEmail(token);
        });
    }

    @Test
    @DisplayName("리프레시 토큰 검증")
    void validateRefreshToken() {
        String refreshToken = jwtTokenProvider.createTokenInfo("test@example.com").refreshToken();

        assertEquals(TokenStatus.VALID, jwtTokenProvider.validateRefreshToken(refreshToken));
    }

    @Test
    @DisplayName("이메일 추출 성공")
    void extractEmailSuccess() {
        String email = "test@example.com";
        String token = jwtTokenProvider.createTokenInfo(email).accessToken();

        assertEquals(email, jwtTokenProvider.extractEmail(token));
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 실패")
    void validateInvalidToken() {
        String invalidToken = "invalid.token.format";

        assertThrows(ResponseStatusException.class, () -> {
            jwtTokenProvider.validateAndGetEmail(invalidToken);
        });
    }
}