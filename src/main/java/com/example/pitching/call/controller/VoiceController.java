package com.example.pitching.call.controller;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.dto.response.UrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoiceController {

    private final ServerProperties serverProperties;

    @GetMapping(value = "/call", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<UrlResponse> getConnectUrl() {
        return Mono.just(UrlResponse.of(serverProperties.getUrl()));
    }
}
