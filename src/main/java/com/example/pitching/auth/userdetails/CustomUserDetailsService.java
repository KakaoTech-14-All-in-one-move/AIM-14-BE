package com.example.pitching.auth.userdetails;

import com.example.pitching.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
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
                        new UsernameNotFoundException("User not found with email: " + email)
                ))
                .map(CustomUserDetails::new);
    }
}