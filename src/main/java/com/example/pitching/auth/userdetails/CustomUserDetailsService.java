package com.example.pitching.auth.userdetails;

import com.example.pitching.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@Primary
@RequiredArgsConstructor
public class CustomUserDetailsService implements ReactiveUserDetailsService {
    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 이메일을 가진 사용자를 찾을 수 없습니다.")
                ))
                .map(CustomUserDetails::new)
                .cast(UserDetails.class)
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 정보 조회 중 오류가 발생했습니다."));
    }
}