package com.example.pitching.auth.controller;

import com.example.pitching.auth.dto.LoginRequest;
import com.example.pitching.auth.dto.RefreshRequest;
import com.example.pitching.auth.dto.TokenInfo;
import com.example.pitching.auth.service.AuthService;
import com.example.pitching.auth.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

// TODO: 경로나 로직 고민
// TODO: access token 만료, refresh token 만료
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public Mono<TokenInfo> login(@RequestBody LoginRequest request) {
        return authService.authenticate(request.email(), request.password());
    }

    @PostMapping("/refresh")
    public Mono<TokenInfo> refresh(@RequestBody RefreshRequest request) {
        return authService.refreshToken(request.refreshToken());
    }
}