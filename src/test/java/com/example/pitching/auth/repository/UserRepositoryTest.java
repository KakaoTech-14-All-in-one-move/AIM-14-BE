package com.example.pitching.auth.repository;

import com.example.pitching.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_USERNAME = "testUser";
    private final String TEST_PASSWORD = "password123";
    private final String TEST_ROLE = "USER";

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리 (순서 중요: 외래 키 제약조건 고려)
        databaseClient.sql("DELETE FROM user_server_memberships").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM users").fetch().rowsUpdated().block();
    }

    @Test
    @DisplayName("이메일로 사용자를 조회한다")
    void findByEmail() {
        // given
        createTestUser();

        // when & then
        StepVerifier.create(userRepository.findByEmail(TEST_EMAIL))
                .assertNext(user -> {
                    assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
                    assertThat(user.getUsername()).isEqualTo(TEST_USERNAME);
                    assertThat(user.getRole()).isEqualTo(TEST_ROLE);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 빈 Mono를 반환한다")
    void findByEmail_NotFound() {
        // when & then
        StepVerifier.create(userRepository.findByEmail("nonexistent@example.com"))
                .verifyComplete();
    }

    @Test
    @DisplayName("이메일의 존재 여부를 확인한다")
    void existsByEmail() {
        // given
        createTestUser();

        // when & then
        StepVerifier.create(userRepository.existsByEmail(TEST_EMAIL))
                .assertNext(exists -> assertThat(exists).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 확인하면 false를 반환한다")
    void existsByEmail_NotFound() {
        // when & then
        StepVerifier.create(userRepository.existsByEmail("nonexistent@example.com"))
                .assertNext(exists -> assertThat(exists).isFalse())
                .verifyComplete();
    }

    @Test
    @DisplayName("새로운 사용자를 등록한다")
    void insertUser() {
        // when
        StepVerifier.create(userRepository.insertUser(TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD, TEST_ROLE)
                        .then(userRepository.findByEmail(TEST_EMAIL)))
                .assertNext(user -> {
                    assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
                    assertThat(user.getUsername()).isEqualTo(TEST_USERNAME);
                    assertThat(user.getPassword()).isEqualTo(TEST_PASSWORD);
                    assertThat(user.getRole()).isEqualTo(TEST_ROLE);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 사용자 등록을 시도하면 실패한다")
    void insertUser_DuplicateEmail() {
        // given
        createTestUser();

        // when & then
        StepVerifier.create(userRepository.insertUser(TEST_EMAIL, "newUser", "newPassword", TEST_ROLE))
                .expectError()
                .verify();
    }

    private void createTestUser() {
        userRepository.insertUser(TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD, TEST_ROLE).block();
    }
}