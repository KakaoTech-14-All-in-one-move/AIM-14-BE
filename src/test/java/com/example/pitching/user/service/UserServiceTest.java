package com.example.pitching.user.service;

import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FilePart filePart;

    @InjectMocks
    private UserService userService;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_USERNAME = "testUser";
    private final String NEW_USERNAME = "newTestUser";
    private final String OLD_IMAGE_URL = "old-image.jpg";
    private final String NEW_IMAGE_URL = "new-image.jpg";

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.createNewUser(TEST_EMAIL, TEST_USERNAME, OLD_IMAGE_URL, "password");

        lenient().when(userRepository.findByEmail(TEST_EMAIL))
                .thenReturn(Mono.just(testUser));
        lenient().when(fileStorageService.store(any(FilePart.class)))
                .thenReturn(Mono.just(NEW_IMAGE_URL));
        lenient().when(userRepository.save(any(User.class)))
                .thenReturn(Mono.just(testUser));
        lenient().when(fileStorageService.delete(any(String.class)))
                .thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("프로필 이미지 업데이트에 성공하면 새로운 이미지 URL을 반환한다")
    void updateProfileImage_Success() {
        // when & then
        StepVerifier.create(userService.updateProfileImage(TEST_EMAIL, filePart))
                .assertNext(url ->
                        assertThat(url).isEqualTo(NEW_IMAGE_URL)
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 프로필 이미지 업데이트 시 NotFound 에러를 반환한다")
    void updateProfileImage_UserNotFound() {
        // given
        when(userRepository.findByEmail("nonexistent@email.com"))
                .thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(userService.updateProfileImage("nonexistent@email.com", filePart))
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isInstanceOf(ResponseStatusException.class)
                                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND)
                )
                .verify();
    }

    @Test
    @DisplayName("사용자명 업데이트에 성공하면 업데이트된 UserResponse를 반환한다")
    void updateUsername_Success() {
        // when & then
        StepVerifier.create(userService.updateUsername(TEST_EMAIL, NEW_USERNAME))
                .assertNext(response ->
                        assertThat(response.username()).isEqualTo(NEW_USERNAME)
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 사용자명 업데이트 시 NotFound 에러를 반환한다")
    void updateUsername_UserNotFound() {
        // given
        when(userRepository.findByEmail("nonexistent@email.com"))
                .thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(userService.updateUsername("nonexistent@email.com", NEW_USERNAME))
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isInstanceOf(ResponseStatusException.class)
                                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND)
                )
                .verify();
    }

    @Test
    @DisplayName("회원 탈퇴 시 사용자 데이터가 성공적으로 삭제된다")
    void withdrawUser_Success() {
        // given
        when(userRepository.delete(any(User.class)))
                .thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(userService.withdrawUser(TEST_EMAIL))
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 회원 탈퇴 시 NotFound 에러를 반환한다")
    void withdrawUser_UserNotFound() {
        // given
        when(userRepository.findByEmail("nonexistent@email.com"))
                .thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(userService.withdrawUser("nonexistent@email.com"))
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isInstanceOf(ResponseStatusException.class)
                                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND)
                )
                .verify();
    }

    @Test
    @DisplayName("프로필 이미지 업데이트 시 이전 이미지가 삭제된다")
    void updateProfileImage_DeletesOldImage() {
        // when
        userService.updateProfileImage(TEST_EMAIL, filePart).block();

        // then
        verify(fileStorageService).delete(OLD_IMAGE_URL);
    }
}