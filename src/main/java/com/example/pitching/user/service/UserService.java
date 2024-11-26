package com.example.pitching.user.service;

import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import com.example.pitching.auth.domain.User;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    private <T> Mono<T> mapCommonError(Mono<T> mono) {
        return mono
                .onErrorMap(DataIntegrityViolationException.class,
                        e -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "중복된 데이터가 존재합니다."))
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 오류가 발생했습니다."));
    }

    public Mono<String> updateProfileImage(String email, FilePart file) {
        return findUser(email)
                .flatMap(user -> storeNewImage(file)
                        .flatMap(newImageUrl -> updateUserAndHandleOldImage(user, newImageUrl)))
                .transform(this::mapCommonError);
    }

    public Mono<UserResponse> updateUsername(String email, String newUsername) {
        return findUser(email)
                .flatMap(user -> updateUserUsername(user, newUsername))
                .map(UserResponse::from)
                .transform(this::mapCommonError);
    }

    public Mono<Void> withdrawUser(String email) {
        return findUser(email)
                .flatMap(this::deleteUserData)
                .transform(this::mapCommonError);
    }

    private Mono<User> findUser(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")
                ));
    }

    private Mono<String> storeNewImage(FilePart file) {
        return fileStorageService.store(file)
                .onErrorMap(error -> error instanceof ResponseStatusException ? error :
                        new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "이미지 저장 중 오류가 발생했습니다: " + error.getMessage())
                );
    }

    private Mono<User> updateUserUsername(User user, String newUsername) {
        user.setUsername(newUsername);
        return userRepository.save(user);
    }

    private Mono<String> updateUserAndHandleOldImage(User user, String newImageUrl) {
        String oldImageUrl = user.getProfileImage();
        user.setProfileImage(newImageUrl);

        return userRepository.save(user)
                .map(User::getProfileImage)
                .doOnNext(__ -> deleteOldImageIfExists(oldImageUrl));
    }

    private Mono<Void> deleteUserData(User user) {
        return Mono.justOrEmpty(user.getProfileImage())
                .flatMap(fileStorageService::delete)
                .then(userRepository.delete(user));
    }

    private void deleteOldImageIfExists(String oldImageUrl) {
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            fileStorageService.delete(oldImageUrl)
                    .doOnError(error -> log.error("Failed to delete old profile image: {}", oldImageUrl, error))
                    .onErrorMap(e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이전 이미지 삭제 중 오류가 발생했습니다."))
                    .subscribe();
        }
    }
}