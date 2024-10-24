package com.example.pitching.auth.controller;

import com.example.pitching.auth.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.security.Principal;

// TODO: 경로나 로직 고민
// TODO: access token 만료, refresh token 만료
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/token")
    public Mono<TokenResponse> getToken(@AuthenticationPrincipal Principal principal) {
        return Mono.just(new TokenResponse(jwtTokenProvider.createToken(principal.getName())));
    }
}

// TODO: 디렉토리 변경
record TokenResponse(String token) {}
