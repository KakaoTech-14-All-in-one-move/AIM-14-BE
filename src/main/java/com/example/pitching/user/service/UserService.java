package com.example.pitching.user.service;

import com.example.pitching.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import com.example.pitching.auth.domain.User;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public Mono<String> updateProfileImage(String email, FilePart file) {
        return findUser(email)
                .flatMap(user -> storeNewImage(file)
                        .flatMap(newImageUrl -> updateUserAndHandleOldImage(user, newImageUrl)));
    }

    public Mono<String> updateUsername(String email, String newUsername) {
        return findUser(email)
                .flatMap(user -> updateUserUsername(user, newUsername));
    }

    public Mono<Void> withdrawUser(String email) {
        return findUser(email)
                .flatMap(this::deleteUserData);
    }

    private Mono<Void> deleteUserData(User user) {
        // TODO: 연관된 서버는 삭제해야 함. 작성한 메시지도 삭제
        // TODO: 삭제를 안할 경우 회원에 대한 정보는 유지해야 함.

        return Mono.justOrEmpty(user.getProfileImage())
                // 프로필 이미지가 있으면 삭제, 없으면 그냥 넘어감
                .flatMap(imageUrl -> fileStorageService.delete(imageUrl))
                .then(userRepository.delete(user))
                .doOnSuccess(__ -> log.info("User successfully withdrawn. Email: {}", user.getEmail()));
    }

    private Mono<User> deleteProfileImage(User user) {
        return Mono.justOrEmpty(user.getProfileImage())
                .flatMap(imageUrl -> fileStorageService.delete(imageUrl)
                        .thenReturn(user))
                .defaultIfEmpty(user);
    }

    private Mono<User> findUser(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(
                        // TODO: Global exception으로 변경
                        // new ResourceNotFoundException("User not found with email: " + email)
                        new Error("User not found with email: " + email)
                ));
    }

    private Mono<String> updateUserUsername(User user, String newUsername) {
        return Mono.just(user)
                .doOnNext(existingUser -> existingUser.setUsername(newUsername))
                .flatMap(userRepository::save)
                .map(User::getUsername)
                .doOnSuccess(__ -> log.info("Updated username for user: {}", user.getEmail()));
    }

    private Mono<String> updateUserProfileImage(User user, FilePart file) {
        return storeNewImage(file)
                .flatMap(newImageUrl -> updateUserAndHandleOldImage(user, newImageUrl));
    }

    private Mono<String> updateUserImage(User user, String newImageUrl) {
        return Mono.just(user)
                .map(u -> {
                    u.setProfileImage(newImageUrl);
                    return u;
                })
                .flatMap(userRepository::save)
                .map(User::getProfileImage)
                .doOnSuccess(__ -> log.info("Updated profile image for user: {}", user.getEmail()));
    }

    private Mono<String> updateUserAndHandleOldImage(User user, String newImageUrl) {
        String oldImageUrl = user.getProfileImage();

        return Mono.just(user)
                .doOnNext(u -> u.setProfileImage(newImageUrl))
                .flatMap(userRepository::save)
                .map(User::getProfileImage)
                .doOnSuccess(__ -> {
                    if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                        deleteOldImage(oldImageUrl);
                    }
                })
                .doOnSuccess(__ -> log.info("Updated profile image for user: {}", user.getEmail()));
    }

    private Mono<String> storeNewImage(FilePart file) {
        return fileStorageService.store(file)
                .onErrorMap(error ->
                        new Error("Failed to store new profile image: " + error.getMessage())
                );
    }

    private void deleteOldImage(String imageUrl) {
        fileStorageService.delete(imageUrl)
                .doOnSuccess(__ -> log.info("Successfully deleted old profile image: {}", imageUrl))
                .doOnError(error -> log.error("Failed to delete old profile image: {}", imageUrl, error))
                .subscribe();
    }
}
