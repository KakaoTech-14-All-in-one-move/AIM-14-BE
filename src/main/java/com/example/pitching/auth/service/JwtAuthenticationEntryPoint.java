package com.example.pitching.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return Mono.fromRunnable(() -> {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        }).then(
                exchange.getResponse().writeWith(Mono.just(
                        exchange.getResponse().bufferFactory().wrap(
                                createErrorMessage(ex).getBytes(StandardCharsets.UTF_8)
                        )
                ))
        );
    }

    private String createErrorMessage(AuthenticationException ex) {
        return String.format(
                "{\"error\": \"%s\", \"status\": %d}",
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED.value()
        );
    }
}