package com.example.pitching.auth.controller;

import com.example.pitching.auth.dto.*;
import com.example.pitching.auth.service.AuthService;
import com.example.pitching.auth.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public Mono<LoginResponse> login(@RequestBody LoginRequest request) {
        return authService.authenticate(request.email(), request.password());
    }

    @PostMapping("/refresh")
    public Mono<TokenInfo> refresh(@RequestBody RefreshRequest request) {
        return authService.refreshToken(request.refreshToken());
    }

    @GetMapping("/check")
    public Mono<ExistsEmailResponse> existsEmail(@RequestParam String email) {
        return authService.existsEmail(email);
    }

    @PostMapping("/signup")
    public Mono<Void> signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
    }
}