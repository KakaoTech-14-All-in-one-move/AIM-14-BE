package com.example.pitching.auth.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("비밀번호 암호화 및 검증 테스트")
class BCryptPasswordEncoderTest {

    private BCryptPasswordEncoder encoder;
    private final String rawPassword = "password123";

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
    }

    @Test
    @DisplayName("비밀번호 암호화 후 검증 시 일치해야 한다")
    void verifyPasswordMatch() {
        // given
        String hashedPassword = encoder.encode(rawPassword);

        // when
        boolean matches = encoder.matches(rawPassword, hashedPassword);

        // then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("다른 비밀번호로 검증 시 일치하지 않아야 한다")
    void verifyPasswordMismatch() {
        // given
        String hashedPassword = encoder.encode(rawPassword);
        String wrongPassword = "wrongPassword";

        // when
        boolean matches = encoder.matches(wrongPassword, hashedPassword);

        // then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("같은 비밀번호라도 매번 다른 해시값이 생성되어야 한다")
    void generateDifferentHashForSamePassword() {
        // given
        String firstHash = encoder.encode(rawPassword);
        String secondHash = encoder.encode(rawPassword);

        // then
        assertThat(firstHash).isNotEqualTo(secondHash);
    }
}