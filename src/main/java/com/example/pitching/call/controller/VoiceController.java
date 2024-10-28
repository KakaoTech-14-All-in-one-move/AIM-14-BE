package com.example.pitching.call.controller;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.dto.response.UrlResponse;
import com.example.pitching.call.handler.VoiceStateManager;
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

    // TODO: 프론트에서 요청할 필요가 없다고 느껴지면 삭제할 수도 있음
    private final ServerProperties serverProperties;
    private final VoiceStateManager voiceStateManager;

    @GetMapping(value = "/call", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<UrlResponse> getConnectUrl() {
        return Mono.just(UrlResponse.of(serverProperties.getUrl()));
    }

    @GetMapping("/state")       // Test
    public Mono<Boolean> addState(String userId, String serverId) {
        return voiceStateManager.enterServer(userId, serverId);
    }
}
