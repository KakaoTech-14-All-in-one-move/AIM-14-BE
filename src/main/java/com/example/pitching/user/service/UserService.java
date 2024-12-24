package com.example.pitching.user.service;

import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.user.dto.UserResponse;
import com.example.pitching.chat.handler.ChatWebSocketHandler;
import com.example.pitching.chat.dto.UserUpdateMessage;
import com.example.pitching.chat.repository.ChatRepository;
import com.example.pitching.chat.repository.ChatRedisRepository;
import com.example.pitching.auth.domain.User;
import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.dto.ChatMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final S3FileStorageService fileStorageService;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatRepository chatRepository;
    private final ChatRedisRepository chatRedisRepository;

    private <T> Mono<T> mapCommonError(Mono<T> mono) {
        return mono
                .onErrorMap(DataIntegrityViolationException.class,
                        e -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "중복된 데이터가 존재합니다."))
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 오류가 발생했습니다."));
    }

    public Mono<String> updateProfileImage(String email, FilePart file) {
        return findUser(email)
                .flatMap(user -> fileStorageService.store(file)
                        .flatMap(newImageUrl -> updateUserAndHandleOldImage(user, newImageUrl)
                                .flatMap(updatedUser -> updateUserMessagesInDatabaseAndCache(
                                        updatedUser.getEmail(),
                                        updatedUser.getUsername(),
                                        newImageUrl
                                ).thenReturn(newImageUrl))
                        ))
                .doOnSuccess(newImageUrl ->
                        findUser(email)
                                .flatMap(user -> {
                                    UserUpdateMessage updateMessage = new UserUpdateMessage(
                                            user.getEmail(),
                                            user.getUsername(),
                                            newImageUrl
                                    );
                                    return chatWebSocketHandler.broadcastUserUpdate(updateMessage);
                                })
                                .subscribe(
                                        null,
                                        error -> log.error("Error broadcasting profile update", error)
                                )
                )
                .transform(this::mapCommonError);
    }

    public Mono<UserResponse> updateUsername(String email, String newUsername) {
        return findUser(email)
                .flatMap(user -> updateUserUsername(user, newUsername))
                .flatMap(updatedUser -> updateUserMessagesInDatabaseAndCache(
                        updatedUser.getEmail(),
                        newUsername,
                        updatedUser.getProfileImage()
                ).thenReturn(updatedUser))
                .doOnSuccess(user -> {
                    UserUpdateMessage updateMessage = new UserUpdateMessage(
                            user.getEmail(),
                            user.getUsername(),
                            user.getProfileImage()
                    );
                    chatWebSocketHandler.broadcastUserUpdate(updateMessage)
                            .subscribe(
                                    null,
                                    error -> log.error("Error broadcasting username update", error)
                            );
                })
                .map(UserResponse::from)
                .transform(this::mapCommonError);
    }

    private Mono<Void> updateUserMessagesInDatabaseAndCache(String email, String newUsername, String newProfileImage) {
        return chatRepository.findBySender(email)
                .flatMap(message -> {
                    // DynamoDB에서 메시지 업데이트
                    return chatRepository.save(message)
                            .thenReturn(ChatMessageDTO.from(message, User.createNewUser(
                                    email,
                                    newUsername,
                                    newProfileImage,
                                    null
                            )));
                })
                .flatMap(updatedMessage ->
                        chatRedisRepository.updateUserMessages(
                                updatedMessage.getChannelId(),
                                email,
                                newUsername,
                                newProfileImage
                        )
                )
                .then();
    }

    private Mono<User> findUser(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")
                ));
    }

    private Mono<User> updateUserAndHandleOldImage(User user, String newImageUrl) {
        String oldImageUrl = user.getProfileImage();
        user.setProfileImage(newImageUrl);

        Mono<Void> deleteOldImage = oldImageUrl != null && !oldImageUrl.isEmpty()
                ? fileStorageService.delete(oldImageUrl)
                : Mono.empty();

        return deleteOldImage
                .then(userRepository.save(user));
    }

    private Mono<User> updateUserUsername(User user, String newUsername) {
        user.setUsername(newUsername);
        return userRepository.save(user);
    }

    public Mono<Void> withdrawUser(String email) {
        return findUser(email)
                .flatMap(this::deleteUserData)
                .transform(this::mapCommonError);
    }

    private Mono<Void> deleteUserData(User user) {
        return Mono.justOrEmpty(user.getProfileImage())
                .flatMap(fileStorageService::delete)
                .then(userRepository.delete(user));
    }

    public Mono<UserResponse> getUser(String email) {
        return findUser(email)
                .map(UserResponse::from)
                .transform(this::mapCommonError);
    }
}