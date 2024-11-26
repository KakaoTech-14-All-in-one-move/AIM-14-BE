package com.example.pitching.auth.controller;

import com.example.pitching.auth.dto.*;
import com.example.pitching.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.authenticate(request.email(), request.password());
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public Mono<TokenInfo> refresh(
            @Valid @RequestBody RefreshRequest request) {
        return authService.refreshToken(request.refreshToken());
    }

    @GetMapping("/check")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ExistsEmailResponse> existsEmail(
            @RequestParam("email") @Email String email) {
        return authService.existsEmail(email);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }
}