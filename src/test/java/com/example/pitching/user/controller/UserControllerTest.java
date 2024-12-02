package com.example.pitching.user.controller;

import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.jwt.JwtTokenProvider;
import com.example.pitching.auth.userdetails.CustomUserDetails;
import com.example.pitching.auth.userdetails.CustomUserDetailsService;
import com.example.pitching.config.SecurityTestConfig;
import com.example.pitching.user.dto.UpdateUsernameRequest;
import com.example.pitching.user.dto.UserResponse;
import com.example.pitching.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(UserController.class)
@Import(SecurityTestConfig.class)
class UserControllerTest {
    private static final String BASE_URL = "/api/v1/users";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    static class TestUserBuilder {
        private String email = "test@example.com";
        private String username = "testUser";
        private String password = "password123";
        private String profileImage = "profile.jpg";

        public TestUserBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public TestUserBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        public TestUserBuilder withProfileImage(String profileImage) {
            this.profileImage = profileImage;
            return this;
        }

        public User build() {
            return User.createNewUser(email, username, profileImage, password);
        }

        public UserResponse buildResponse() {
            User user = build();
            return new UserResponse(user.getEmail(), user.getUsername(), user.getProfileImage());
        }
    }

    @Nested
    @DisplayName("프로필 이미지 업데이트 API")
    class UpdateProfileImageTests {
        @Test
        @DisplayName("유효한 이미지로 프로필 업데이트 시 성공")
        void shouldUpdateProfileImageSuccessfully() {
            // given
            User testUser = new TestUserBuilder().build();
            String expectedImageUrl = "updated-profile.jpg";
            setupAuthenticatedUser(testUser);
            Resource testImage = createTestImageResource();

            when(userService.updateProfileImage(eq(testUser.getEmail()), any(FilePart.class)))
                    .thenReturn(Mono.just(expectedImageUrl));

            // when & then
            webTestClient.post()
                    .uri(BASE_URL + "/profile-image")
                    .headers(headers -> headers.setBearerAuth("valid-token"))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData("file", testImage))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.profileImageUrl").isEqualTo(expectedImageUrl);
        }

        @Test
        @DisplayName("인증되지 않은 사용자의 프로필 이미지 업데이트 시도 시 Unauthorized 반환")
        void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() {
            // Create test image data
            Resource testImage = createTestImageResource();

            // when & then
            webTestClient.post()
                    .uri(BASE_URL + "/profile-image")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData("file", testImage))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("파일이 누락된 경우 BadRequest 반환")
        void shouldReturnBadRequestWhenFileIsMissing() {
            // given
            User testUser = new TestUserBuilder().build();
            setupAuthenticatedUser(testUser);
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();

            // when & then
            webTestClient.post()
                    .uri(BASE_URL + "/profile-image")
                    .headers(headers -> headers.setBearerAuth("valid-token"))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(bodyBuilder.build())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").exists();
        }

        @Test
        @DisplayName("파일 크기가 너무 큰 경우 PayloadTooLarge 반환")
        void shouldReturnPayloadTooLargeWhenFileExceedsMaxSize() {
            // given
            User testUser = new TestUserBuilder().build();
            setupAuthenticatedUser(testUser);

            // Create a large byte array to simulate a large file
            byte[] largeFile = new byte[5 * 1024 * 1024]; // 5MB

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(largeFile) {
                @Override
                public String getFilename() {
                    return "large-file.jpg";
                }
            });

            when(userService.updateProfileImage(eq(testUser.getEmail()), any(FilePart.class)))
                    .thenReturn(Mono.error(new ResponseStatusException(
                            HttpStatus.PAYLOAD_TOO_LARGE,
                            "파일 크기가 제한을 초과했습니다. (최대 2MB)"
                    )));

            // when & then
            webTestClient.post()
                    .uri(BASE_URL + "/profile-image")
                    .headers(headers -> headers.setBearerAuth("valid-token"))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(body)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE)
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("파일 크기가 제한을 초과했습니다. (최대 2MB)");
        }

        @Test
        @DisplayName("지원하지 않는 파일 형식 업로드 시 BadRequest 반환")
        void shouldReturnBadRequestWhenFileTypeIsNotSupported() {
            // given
            User testUser = new TestUserBuilder().build();
            setupAuthenticatedUser(testUser);

            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", new ByteArrayResource("test-image".getBytes()) {
                @Override
                public String getFilename() {
                    return "test.txt"; // Using unsupported file type
                }
            }).contentType(MediaType.TEXT_PLAIN);

            when(userService.updateProfileImage(eq(testUser.getEmail()), any(FilePart.class)))
                    .thenReturn(Mono.error(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "지원하지 않는 파일 형식입니다."
                    )));

            // when & then
            webTestClient.post()
                    .uri(BASE_URL + "/profile-image")
                    .headers(headers -> headers.setBearerAuth("valid-token"))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("지원하지 않는 파일 형식입니다.");
        }
    }

    @Nested
    @DisplayName("사용자 이름 업데이트 API")
    class UpdateUsernameTests {
        @Test
        @DisplayName("유효한 새 이름으로 업데이트 시 성공")
        void shouldUpdateUsernameSuccessfully() {
            // given
            User originalUser = new TestUserBuilder().build();
            String newUsername = "newUsername";
            UserResponse updatedResponse = new TestUserBuilder()
                    .withUsername(newUsername)
                    .buildResponse();

            setupAuthenticatedUser(originalUser);
            when(userService.updateUsername(originalUser.getEmail(), newUsername))
                    .thenReturn(Mono.just(updatedResponse));

            // when & then
            webTestClient.put()
                    .uri(BASE_URL + "/username")
                    .headers(headers -> headers.setBearerAuth("valid-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new UpdateUsernameRequest(newUsername))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.username").isEqualTo(newUsername)
                    .jsonPath("$.email").isEqualTo(originalUser.getEmail());
        }

        @Test
        @DisplayName("인증되지 않은 사용자의 이름 업데이트 시도 시 Unauthorized 반환")
        void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() {
            // when & then
            webTestClient.put()
                    .uri(BASE_URL + "/username")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new UpdateUsernameRequest("newUsername"))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("중복된 사용자 이름으로 업데이트 시 Conflict 반환")
        void shouldReturnConflictWhenUsernameIsDuplicate() {
            // given
            User testUser = new TestUserBuilder().build();
            setupAuthenticatedUser(testUser);
            String duplicateUsername = "existingUsername";

            when(userService.updateUsername(testUser.getEmail(), duplicateUsername))
                    .thenReturn(Mono.error(new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "이미 사용 중인 사용자 이름입니다."
                    )));

            // when & then
            webTestClient.put()
                    .uri(BASE_URL + "/username")
                    .headers(headers -> headers.setBearerAuth("valid-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new UpdateUsernameRequest(duplicateUsername))
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("이미 사용 중인 사용자 이름입니다.");
        }

        @Test
        @DisplayName("너무 짧은 사용자 이름으로 업데이트 시 BadRequest 반환")
        void shouldReturnBadRequestWhenUsernameIsTooShort() {
            // given
            User testUser = new TestUserBuilder().build();
            setupAuthenticatedUser(testUser);
            String shortUsername = "a"; // 2자 미만

            // when & then
            webTestClient.put()
                    .uri(BASE_URL + "/username")
                    .headers(headers -> headers.setBearerAuth("valid-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new UpdateUsernameRequest(shortUsername))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").exists();
        }

        @Test
        @DisplayName("너무 긴 사용자 이름으로 업데이트 시 BadRequest 반환")
        void shouldReturnBadRequestWhenUsernameIsTooLong() {
            // given
            User testUser = new TestUserBuilder().build();
            setupAuthenticatedUser(testUser);
            String longUsername = "a".repeat(51); // 50자 초과

            // when & then
            webTestClient.put()
                    .uri(BASE_URL + "/username")
                    .headers(headers -> headers.setBearerAuth("valid-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new UpdateUsernameRequest(longUsername))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").exists();
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 API")
    class WithdrawUserTests {
        @Test
        @DisplayName("회원 탈퇴 성공")
        void shouldWithdrawUserSuccessfully() {
            // given
            User testUser = new TestUserBuilder().build();
            setupAuthenticatedUser(testUser);
            when(userService.withdrawUser(testUser.getEmail()))
                    .thenReturn(Mono.empty());

            // when & then
            webTestClient.delete()
                    .uri(BASE_URL + "/me")
                    .headers(headers -> headers.setBearerAuth("valid-token"))
                    .exchange()
                    .expectStatus().isNoContent();
        }

        @Test
        @DisplayName("인증되지 않은 사용자의 회원 탈퇴 시도 시 Unauthorized 반환")
        void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() {
            // when & then
            webTestClient.delete()
                    .uri(BASE_URL + "/me")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("회원 탈퇴 중 서버 에러 발생 시 ServerError 반환")
        void shouldReturnServerErrorWhenWithdrawalFails() {
            // given
            User testUser = new TestUserBuilder().build();
            setupAuthenticatedUser(testUser);
            when(userService.withdrawUser(testUser.getEmail()))
                    .thenReturn(Mono.error(new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "회원 탈퇴 처리 중 오류가 발생했습니다."
                    )));

            // when & then
            webTestClient.delete()
                    .uri(BASE_URL + "/me")
                    .headers(headers -> headers.setBearerAuth("valid-token"))
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("회원 탈퇴 처리 중 오류가 발생했습니다.");
        }
    }

    private void setupAuthenticatedUser(User user) {
        when(jwtTokenProvider.validateAndGetEmail("valid-token"))
                .thenReturn(user.getEmail());
        when(userDetailsService.findByUsername(user.getEmail()))
                .thenReturn(Mono.just(new CustomUserDetails(user)));
    }

    private MultipartBodyBuilder createMultipartBody() {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", createTestImageResource())
                .contentType(MediaType.IMAGE_JPEG);
        return bodyBuilder;
    }

    private Resource createTestImageResource() {
        return new ByteArrayResource("test-image".getBytes()) {
            @Override
            public String getFilename() {
                return "test-image.jpg";
            }
        };
    }
}