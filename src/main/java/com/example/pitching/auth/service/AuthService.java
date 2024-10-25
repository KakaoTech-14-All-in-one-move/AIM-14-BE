package com.example.pitching.auth.service;

import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.dto.TokenInfo;
import com.example.pitching.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MapReactiveUserDetailsService userDetailsService; // userRepository 대신 사용
    //private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public Mono<TokenInfo> authenticate(String username, String password) {
        return userDetailsService.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> user.getUsername())
                .map(jwtTokenProvider::createTokenInfo)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invalid credentials")));
    }
}

/*
package com.example.pitching.auth.service;

import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.dto.TokenInfo;
import com.example.pitching.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public Mono<TokenInfo> authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .cast(User.class)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(User::getUsername)
                .map(jwtTokenProvider::createTokenInfo)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invalid credentials")));
    }
}

*/