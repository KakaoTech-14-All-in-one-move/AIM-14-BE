package com.example.pitching.auth.service;

import com.example.pitching.auth.domain.TokenStatus;
import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.dto.TokenInfo;
import com.example.pitching.auth.exception.InvalidCredentialsException;
import com.example.pitching.auth.exception.InvalidTokenException;
import com.example.pitching.auth.exception.TokenExpiredException;
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

    public Mono<TokenInfo> authenticate(String email, String password) {
        return userDetailsService.findByUsername(email)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> user.getUsername())
                .map(jwtTokenProvider::createTokenInfo)
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Invalid credentials")));
    }

    public Mono<TokenInfo> refreshToken(String refreshToken) {
        return validateRefreshToken(refreshToken)
                .flatMap(username -> Mono.just(jwtTokenProvider.recreateAccessToken(username)))
                .onErrorResume(e -> {
                    if (e instanceof TokenExpiredException) {
                        // 재로그인 필요
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Refresh token expired. Please login again."));
                    } else if (e instanceof InvalidTokenException) {
                        // 잘못된 토큰
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid refresh token."));
                    }
                    // 기타 예외
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error processing refresh token."));
                });
    }

    private Mono<String> validateRefreshToken(String refreshToken) {
        return Mono.fromCallable(() -> {
            TokenStatus status = jwtTokenProvider.validateRefreshToken(refreshToken);

            switch (status) {
                case VALID:
                    return jwtTokenProvider.extractUsername(refreshToken);
                case EXPIRED:
                    throw new TokenExpiredException("Refresh token has expired");
                case BLACKLISTED:
                    throw new InvalidTokenException("Refresh token has been revoked");
                case INVALID:
                default:
                    throw new InvalidTokenException("Invalid refresh token");
            }
        });
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