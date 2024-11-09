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
                .flatMap(user -> updateUserProfileImage(user, file));
    }

    public Mono<String> updateUsername(String email, String newUsername) {
        return findUser(email)
                .flatMap(user -> updateUserUsername(user, newUsername));
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
                .map(u -> {
                    u.setUsername(newUsername);
                    return u;
                })
                .flatMap(userRepository::save)
                .map(User::getUsername)
                .doOnSuccess(__ -> log.info("Updated username for user: {}", user.getEmail()));
    }

    private Mono<String> updateUserProfileImage(User user, FilePart file) {
        return storeNewImage(file)
                .flatMap(newImageUrl -> updateUserAndHandleOldImage(user, newImageUrl));
    }

    private Mono<String> storeNewImage(FilePart file) {
        return fileStorageService.store(file)
                .onErrorResume(error -> Mono.error(
                        // TODO: Global exception으로 변경
                        // new ImageProcessingException("Failed to store new profile image", error)
                        new Error("Failed to store new profile image", error)
                ));
    }

    private Mono<String> updateUserAndHandleOldImage(User user, String newImageUrl) {
        return Mono.just(user.getProfileImage())
                .flatMap(oldImageUrl -> {
                    if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                        return updateUserImage(user, newImageUrl)
                                .doOnSuccess(__ -> deleteOldImage(oldImageUrl));
                    }
                    return updateUserImage(user, newImageUrl);
                });
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

    private void deleteOldImage(String imageUrl) {
        fileStorageService.delete(imageUrl)
                .doOnSuccess(__ -> log.info("Successfully deleted old profile image: {}", imageUrl))
                .doOnError(error -> log.error("Failed to delete old profile image: {}", imageUrl, error))
                .subscribe();
    }
}
