package com.example.pitching.user.controller;

import com.example.pitching.auth.jwt.JwtTokenProvider;
import com.example.pitching.auth.userdetails.CustomUserDetailsService;
import com.example.pitching.config.SecurityTestConfig;
import com.example.pitching.user.dto.InviteMemberRequest;
import com.example.pitching.user.dto.ServerRequest;
import com.example.pitching.user.dto.ServerResponse;
import com.example.pitching.user.service.ServerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@WebFluxTest(ServerController.class)
@Import(SecurityTestConfig.class)
class ServerControllerTest {
    private static final String BASE_URL = "/api/v1/servers";
    private static final String VALID_TOKEN = "valid-test-token";
    private static final String TEST_EMAIL = "test@example.com";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ServerService serverService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private ServerRequest testServerRequest;
    private ServerResponse testServerResponse;

    @BeforeEach
    void setUp() {
        testServerRequest = new ServerRequest("Test Server", "https://example.com/image.jpg");
        testServerResponse = new ServerResponse(1L, "Test Server", "https://example.com/image.jpg", null);
        setupAuthentication();
    }

    private void setupAuthentication() {
        when(jwtTokenProvider.validateAndGetEmail(VALID_TOKEN))
                .thenReturn(TEST_EMAIL);

        UserDetails userDetails = User.builder()
                .username(TEST_EMAIL)
                .password("password")
                .roles("USER")
                .build();
        when(userDetailsService.findByUsername(TEST_EMAIL))
                .thenReturn(Mono.just(userDetails));
    }

    private WebTestClient.RequestHeadersSpec<?> authenticatedRequest(
            WebTestClient.RequestHeadersSpec<?> request) {
        return request.headers(headers -> headers.setBearerAuth(VALID_TOKEN));
    }

    @Nested
    @DisplayName("서버 생성 API")
    class CreateServerTests {
        @Test
        @DisplayName("유효한 요청으로 서버 생성 시 성공")
        void shouldCreateServerSuccessfully() {
            when(serverService.createServer(eq(testServerRequest), any()))
                    .thenReturn(Mono.just(testServerResponse));

            authenticatedRequest(webTestClient.post()
                    .uri(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(testServerRequest))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.server_id").isEqualTo(1L)
                    .jsonPath("$.server_name").isEqualTo("Test Server")
                    .jsonPath("$.server_image").isEqualTo("https://example.com/image.jpg");
        }

        @Test
        @DisplayName("서버 생성 실패 - 인증되지 않은 사용자")
        void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() {
            webTestClient.post()
                    .uri(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(testServerRequest)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("서버 이름이 비어있는 경우 BadRequest 반환")
        void shouldReturnBadRequestWhenServerNameIsEmpty() {
            ServerRequest invalidRequest = new ServerRequest("", "https://example.com/image.jpg");

            authenticatedRequest(webTestClient.post()
                    .uri(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(invalidRequest))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").exists();
        }

        @Test
        @DisplayName("잘못된 이미지 URL 형식인 경우 BadRequest 반환")
        void shouldReturnBadRequestWhenImageUrlIsInvalid() {
            ServerRequest invalidRequest = new ServerRequest("Test Server", "invalid-url");

            authenticatedRequest(webTestClient.post()
                    .uri(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(invalidRequest))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").exists();
        }
    }

    @Nested
    @DisplayName("서버 이름 수정 API")
    class UpdateServerNameTests {
        @Test
        @DisplayName("유효한 요청으로 서버 이름 수정 시 성공")
        void shouldUpdateServerNameSuccessfully() {
            Long serverId = 1L;
            ServerRequest request = new ServerRequest("Updated Server", null);
            ServerResponse response = new ServerResponse(serverId, "Updated Server", "https://example.com/image.jpg", null);

            when(serverService.updateServerName(eq(serverId), eq(request.server_name()), any()))
                    .thenReturn(Mono.just(response));

            authenticatedRequest(webTestClient.put()
                    .uri(BASE_URL + "/{server_id}/name", serverId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.server_name").isEqualTo("Updated Server");
        }

        @Test
        @DisplayName("서버 이름이 비어있는 경우 BadRequest 반환")
        void shouldReturnBadRequestWhenServerNameIsEmpty() {
            Long serverId = 1L;
            ServerRequest request = new ServerRequest("", null);

            authenticatedRequest(webTestClient.put()
                    .uri(BASE_URL + "/{server_id}/name", serverId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").exists();
        }
    }

    @Nested
    @DisplayName("멤버 초대 API")
    class InviteMemberTests {
        @Test
        @DisplayName("유효한 이메일로 멤버 초대 시 성공")
        void shouldInviteMemberSuccessfully() {
            Long serverId = 1L;
            InviteMemberRequest request = new InviteMemberRequest("test@example.com");

            when(serverService.inviteMember(eq(serverId), eq(request.email())))
                    .thenReturn(Mono.empty());

            authenticatedRequest(webTestClient.post()
                    .uri(BASE_URL + "/{server_id}/invite", serverId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request))
                    .exchange()
                    .expectStatus().isCreated();
        }

        @Test
        @DisplayName("이미 초대된 멤버 재초대 시 Conflict 반환")
        void shouldReturnConflictWhenMemberAlreadyInvited() {
            Long serverId = 1L;
            InviteMemberRequest request = new InviteMemberRequest("test@example.com");

            when(serverService.inviteMember(eq(serverId), eq(request.email())))
                    .thenReturn(Mono.error(new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "이미 초대된 멤버입니다."
                    )));

            authenticatedRequest(webTestClient.post()
                    .uri(BASE_URL + "/{server_id}/invite", serverId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request))
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("이미 초대된 멤버입니다.");
        }
    }

    @Nested
    @DisplayName("서버 목록 조회 API")
    class GetServersTests {
        @Test
        @DisplayName("사용자의 서버 목록 조회 성공")
        void shouldGetUserServersSuccessfully() {
            List<ServerResponse> servers = List.of(
                    new ServerResponse(1L, "Server 1", "image1.jpg", null),
                    new ServerResponse(2L, "Server 2", "image2.jpg", null)
            );

            when(serverService.getUserServers(any()))
                    .thenReturn(Flux.fromIterable(servers));

            authenticatedRequest(webTestClient.get()
                    .uri(BASE_URL))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ServerResponse.class)
                    .hasSize(2)
                    .contains(servers.toArray(new ServerResponse[0]));
        }

        @Test
        @DisplayName("잘못된 토큰으로 서버 목록 조회 실패")
        void shouldReturnUnauthorizedWhenTokenIsInvalid() {
            String invalidToken = "invalid-token";
            when(jwtTokenProvider.validateAndGetEmail(invalidToken))
                    .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

            webTestClient.get()
                    .uri(BASE_URL)
                    .headers(headers -> headers.setBearerAuth(invalidToken))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    @Nested
    @DisplayName("서버 삭제 API")
    class DeleteServerTests {
        @Test
        @DisplayName("서버 삭제 성공")
        void shouldDeleteServerSuccessfully() {
            Long serverId = 1L;

            when(serverService.deleteServer(eq(serverId), any()))
                    .thenReturn(Mono.empty());

            authenticatedRequest(webTestClient.delete()
                    .uri(BASE_URL + "/{server_id}", serverId))
                    .exchange()
                    .expectStatus().isNoContent();
        }

        @Test
        @DisplayName("존재하지 않는 서버 삭제 시도 시 NotFound 반환")
        void shouldReturnNotFoundWhenServerDoesNotExist() {
            Long serverId = 999L;

            when(serverService.deleteServer(eq(serverId), any()))
                    .thenReturn(Mono.error(new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "존재하지 않는 서버입니다."
                    )));

            authenticatedRequest(webTestClient.delete()
                    .uri(BASE_URL + "/{server_id}", serverId))
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("존재하지 않는 서버입니다.");
        }
    }
}