package com.example.pitching.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HealthController {
    @GetMapping("/health")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> healthCheck() {
        return Mono.empty();
    }
}
