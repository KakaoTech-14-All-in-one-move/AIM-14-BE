package com.example.pitching.user.repository;

import com.example.pitching.user.domain.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@ActiveProfiles("test")
class ServerRepositoryTest {

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private final String TEST_EMAIL = "test@example.com";
    private Server testServer1;
    private Server testServer2;

    @BeforeEach
    void setUp() {
        databaseClient.sql("DELETE FROM user_server_memberships").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM channels").fetch().rowsUpdated().block();
        serverRepository.deleteAll().block();
        databaseClient.sql("DELETE FROM users").fetch().rowsUpdated().block();

        // 사용자 생성
        databaseClient.sql("""
                INSERT INTO users (email, username, role)
                VALUES ($1, $2, $3)
                """)
                .bind(0, TEST_EMAIL)
                .bind(1, "Test User")
                .bind(2, "USER")
                .fetch()
                .rowsUpdated()
                .block();

        // 서버 생성
        testServer1 = serverRepository.save(Server.createNewServer("Test Server 1", null)).block();
        testServer2 = serverRepository.save(Server.createNewServer("Test Server 2", null)).block();
    }

    @Test
    @DisplayName("사용자의 이메일로 참여중인 서버 목록을 조회한다")
    void findServersByUserEmail() {
        // given
        createUserServerMembership(TEST_EMAIL, testServer1.getServerId()).block();
        createUserServerMembership(TEST_EMAIL, testServer2.getServerId()).block();

        // when & then
        StepVerifier.create(serverRepository.findServersByUserEmail(TEST_EMAIL))
                .assertNext(server -> {
                    assertThat(server.getServerName()).isEqualTo("Test Server 1");
                    assertThat(server.getServerId()).isEqualTo(testServer1.getServerId());
                })
                .assertNext(server -> {
                    assertThat(server.getServerName()).isEqualTo("Test Server 2");
                    assertThat(server.getServerId()).isEqualTo(testServer2.getServerId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("서버에 참여하지 않은 사용자의 경우 빈 목록을 반환한다")
    void findServersByUserEmail_NoServers() {
        // when & then
        StepVerifier.create(serverRepository.findServersByUserEmail("nonexistent@example.com"))
                .verifyComplete();
    }

    @Test
    @DisplayName("일부 서버에만 참여한 사용자의 경우 참여한 서버만 반환한다")
    void findServersByUserEmail_PartialMembership() {
        // given
        createUserServerMembership(TEST_EMAIL, testServer1.getServerId()).block();

        // when & then
        StepVerifier.create(serverRepository.findServersByUserEmail(TEST_EMAIL))
                .assertNext(server -> {
                    assertThat(server.getServerName()).isEqualTo("Test Server 1");
                    assertThat(server.getServerId()).isEqualTo(testServer1.getServerId());
                })
                .verifyComplete();
    }

    private Mono<Void> createUserServerMembership(String email, Long serverId) {
        return databaseClient.sql("""
                INSERT INTO user_server_memberships (email, server_id)
                VALUES ($1, $2)
                """)
                .bind(0, email)
                .bind(1, serverId)
                .fetch()
                .rowsUpdated()
                .then();
    }
}