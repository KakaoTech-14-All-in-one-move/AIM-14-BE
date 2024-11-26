package com.example.pitching.auth.service;

import com.example.pitching.auth.domain.TokenStatus;
import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.dto.*;
import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.auth.jwt.JwtTokenProvider;
import com.example.pitching.user.dto.ServerInfo;
import com.example.pitching.user.repository.ChannelRepository;
import com.example.pitching.user.repository.ServerRepository;
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
    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;

    public Mono<LoginResponse> authenticate(String email, String password) {
        return userRepository.findByEmail(email)
                .cast(User.class)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .flatMap(user -> serverRepository.findServersByUserEmail(user.getEmail())
                        .flatMap(server ->
                                channelRepository.findByServerId(server.getServerId())
                                        .collectList()
                                        .map(channels -> new ServerInfo(
                                                server.getServerId(),
                                                server.getServerName(),
                                                server.getServerImage(),
                                                channels
                                        ))
                        )
                        .collectList()
                        .map(servers -> new LoginResponse(
                                jwtTokenProvider.createTokenInfo(user.getEmail()),
                                new UserInfo(
                                        user.getEmail(),
                                        user.getUsername(),
                                        user.getProfileImage(),
                                        servers
                                )
                        ))
                )
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."
                )))
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "로그인 처리 중 오류가 발생했습니다."));
    }

    public Mono<TokenInfo> refreshToken(String refreshToken) {
        return validateRefreshToken(refreshToken)
                .flatMap(email -> Mono.just(jwtTokenProvider.recreateAccessToken(email)))
                .onErrorMap(e -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "리프레시 토큰이 유효하지 않거나 만료되었습니다. 다시 로그인해주세요.")
                );
    }

    private Mono<String> validateRefreshToken(String refreshToken) {
        return Mono.fromCallable(() -> {
            TokenStatus status = jwtTokenProvider.validateRefreshToken(refreshToken);
            return switch (status) {
                case VALID -> jwtTokenProvider.extractEmail(refreshToken);
                case EXPIRED -> throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다.");
                default -> throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
            };
        });
    }

    public Mono<ExistsEmailResponse> existsEmail(String email) {
        return userRepository.existsByEmail(email)
                .map(ExistsEmailResponse::new)
                .onErrorMap(e -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "이메일 중복 확인 중 오류가 발생했습니다."));
    }

    public Mono<Void> signup(SignupRequest request) {
        return userRepository.existsByEmail(request.email())
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다.")
                ))
                .flatMap(notExists -> userRepository.insertUser(
                        request.email(),
                        request.username(),
                        passwordEncoder.encode(request.password()),
                        "USER"
                ))
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR, "회원가입 처리 중 오류가 발생했습니다."));
    }
}