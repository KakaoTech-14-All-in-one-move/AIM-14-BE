package com.example.pitching.user.repository;

import com.example.pitching.user.domain.Server;
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
class UserServerMembershipRepositoryTest {

    @Autowired
    private UserServerMembershipRepository membershipRepository;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private final String TEST_EMAIL = "test@example.com";
    private Long serverId;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        databaseClient.sql("DELETE FROM user_server_memberships").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM channels").fetch().rowsUpdated().block();
        serverRepository.deleteAll().block();
        databaseClient.sql("DELETE FROM users").fetch().rowsUpdated().block();

        // 테스트용 사용자 생성
        databaseClient.sql("INSERT INTO users (email, username, role, user_id) VALUES ($1, $2, $3, DEFAULT)")
                .bind(0, TEST_EMAIL)
                .bind(1, "Test User")
                .bind(2, "USER")
                .fetch()
                .rowsUpdated()
                .block();

        // 테스트용 서버 생성
        Server testServer = serverRepository.save(Server.createNewServer("Test Server", null)).block();
        serverId = testServer.getServerId();
    }

    @Test
    @DisplayName("서버와 이메일로 멤버십을 조회한다")
    void findByServerIdAndEmail() {
        // given
        createMembership(serverId, TEST_EMAIL);

        // when & then
        StepVerifier.create(membershipRepository.findByServerIdAndEmail(serverId, TEST_EMAIL))
                .assertNext(membership -> {
                    assertThat(membership.getEmail()).isEqualTo(TEST_EMAIL);
                    assertThat(membership.getServerId()).isEqualTo(serverId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 멤버십 조회 시 빈 Mono를 반환한다")
    void findByServerIdAndEmail_NotFound() {
        StepVerifier.create(membershipRepository.findByServerIdAndEmail(serverId, TEST_EMAIL))
                .verifyComplete();
    }

    @Test
    @DisplayName("서버의 멤버 수를 조회한다")
    void countByServerId() {
        // given
        createMembership(serverId, TEST_EMAIL);
        createMembership(serverId, "another@example.com");

        // when & then
        StepVerifier.create(membershipRepository.countByServerId(serverId))
                .assertNext(count -> assertThat(count).isEqualTo(2))
                .verifyComplete();
    }

    @Test
    @DisplayName("멤버가 없는 서버의 멤버 수 조회 시 0을 반환한다")
    void countByServerId_NoMembers() {
        StepVerifier.create(membershipRepository.countByServerId(serverId))
                .assertNext(count -> assertThat(count).isZero())
                .verifyComplete();
    }

    @Test
    @DisplayName("멤버십을 삭제한다")
    void deleteByServerIdAndEmail() {
        // given
        createMembership(serverId, TEST_EMAIL);

        // when & then
        StepVerifier.create(membershipRepository.deleteByServerIdAndEmail(serverId, TEST_EMAIL)
                        .then(membershipRepository.findByServerIdAndEmail(serverId, TEST_EMAIL)))
                .verifyComplete();
    }

    @Test
    @DisplayName("사용자 생성 시 user_id가 자동으로 증가한다")
    void userIdAutoIncrement() {
        // when
        databaseClient.sql("INSERT INTO users (email, username, role, user_id) VALUES ($1, $2, $3, DEFAULT)")
                .bind(0, "first@example.com")
                .bind(1, "First User")
                .bind(2, "USER")
                .fetch()
                .rowsUpdated()
                .block();

        databaseClient.sql("INSERT INTO users (email, username, role, user_id) VALUES ($1, $2, $3, DEFAULT)")
                .bind(0, "second@example.com")
                .bind(1, "Second User")
                .bind(2, "USER")
                .fetch()
                .rowsUpdated()
                .block();

        // then
        StepVerifier.create(databaseClient.sql("SELECT user_id FROM users ORDER BY user_id DESC LIMIT 2")
                        .map(row -> row.get("user_id", Long.class))
                        .all()
                        .collectList())
                .assertNext(userIds -> assertThat(userIds.get(0)).isGreaterThan(userIds.get(1)))
                .verifyComplete();
    }

    private void createMembership(Long serverId, String email) {
        // 사용자가 없는 경우 생성
        if (!email.equals(TEST_EMAIL)) {
            databaseClient.sql("INSERT INTO users (email, username, role, user_id) VALUES ($1, $2, $3, DEFAULT)")
                    .bind(0, email)
                    .bind(1, "Test User")
                    .bind(2, "USER")
                    .fetch()
                    .rowsUpdated()
                    .block();
        }

        databaseClient.sql("INSERT INTO user_server_memberships (server_id, email) VALUES ($1, $2)")
                .bind(0, serverId)
                .bind(1, email)
                .fetch()
                .rowsUpdated()
                .block();
    }
}