package com.example.pitching.voice.controller;

import com.example.pitching.voice.dto.response.UrlRes;
import com.example.pitching.voice.dto.properties.ServerProperties;
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

    @GetMapping(value = "/gateway", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<UrlRes> getGateway() {
        return Mono.just(UrlRes.of(serverProperties.getUrl()));
    }
}
