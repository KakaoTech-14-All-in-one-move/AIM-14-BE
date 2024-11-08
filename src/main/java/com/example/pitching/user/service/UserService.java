package com.example.pitching.user.service;

import com.example.pitching.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import com.example.pitching.auth.domain.User;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public Mono<String> updateProfileImage(String email, FilePart file) {
        return fileStorageService.store(file)
                .flatMap(imageUrl -> userRepository.findByEmail(email)
                        .flatMap(user -> {
                            user.setProfileImage(imageUrl);
                            return userRepository.save(user);
                        })
                        .map(User::getProfileImage));
    }
}
