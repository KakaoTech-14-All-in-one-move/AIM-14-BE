package com.example.pitching.gateway.controller;

import com.example.pitching.gateway.dto.properties.ServerProperties;
import com.example.pitching.gateway.dto.response.GatewayRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GatewayController {

    private final ServerProperties serverProperties;

    @GetMapping(value = "/gateway", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<GatewayRes> getGateway() {
        return Mono.just(GatewayRes.of(serverProperties.getUrl()));
    }
}
